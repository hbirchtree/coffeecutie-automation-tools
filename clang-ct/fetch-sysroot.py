#!/usr/bin/env python3
"""
Assemble a glibc sysroot from Debian-style .deb packages.

Resolves the requested packages (and their Depends/Pre-Depends) from one or
more repositories' Packages indices, verifies SHA256, extracts them and makes
the result usable as a --sysroot: absolute symlinks are made relative and
files irrelevant to cross compiling are pruned.

Repositories are given as "URL SUITE COMPONENT[,COMPONENT...]". When several
repositories carry a package, the one listed last wins, so list overlays such
as -security or archive.raspberrypi.org after the base distribution.
"""

import argparse
import hashlib
import lzma
import os
import re
import shutil
import subprocess
import sys
import urllib.request

# Runtime/packaging-only packages that pull in large dependency trees without
# contributing anything to a sysroot
DEFAULT_EXCLUDES = {
    'adduser', 'debconf', 'debconf-2.0', 'dpkg', 'install-info',
    'init-system-helpers', 'libc-bin', 'libc-dev-bin', 'lsb-base',
    'multiarch-support', 'passwd', 'perl-base', 'sensible-utils',
    'tzdata', 'ucf',
}

PRUNE = [
    'etc', 'sbin', 'var',
    'usr/bin', 'usr/sbin', 'usr/libexec', 'usr/games',
    'usr/share/doc', 'usr/share/man', 'usr/share/info', 'usr/share/locale',
    'usr/share/lintian', 'usr/share/bug', 'usr/share/bash-completion',
]


def fetch(url):
    try:
        with urllib.request.urlopen(url) as response:
            return response.read()
    except urllib.error.HTTPError as e:
        raise RuntimeError(f'{url}: {e}') from None


def parse_packages(index, mirror):
    packages = {}
    for stanza in index.split('\n\n'):
        fields = {}
        key = None
        for line in stanza.splitlines():
            if line and line[0].isspace():
                if key:
                    fields[key] += ' ' + line.strip()
            elif ':' in line:
                key, value = line.split(':', 1)
                fields[key] = value.strip()
        if 'Package' in fields:
            fields['Mirror'] = mirror
            packages[fields['Package']] = fields
    return packages


def load_repos(repos, arch):
    packages = {}
    for repo in repos:
        mirror, suite, components = repo.split()
        for component in components.split(','):
            url = f'{mirror}/dists/{suite}/{component}/binary-{arch}/Packages.xz'
            print(f' * Fetching {url}', file=sys.stderr)
            index = lzma.decompress(fetch(url)).decode('utf-8')
            packages.update(parse_packages(index, mirror))
    provides = {}
    for name, fields in packages.items():
        for virtual in split_relations(fields.get('Provides', '')):
            provides.setdefault(virtual[0], name)
    return packages, provides


def split_relations(field):
    """Return a list of alternatives lists: 'a (>= 1) | b:any, c' -> [[a, b], [c]]"""
    result = []
    for group in field.split(','):
        names = [re.sub(r'[\s(\[<].*$', '', alt.strip()).split(':')[0]
                 for alt in group.split('|')]
        names = [name for name in names if name]
        if names:
            result.append(names)
    return result


def resolve(requested, packages, provides, excludes):
    def lookup(name):
        if name in packages:
            return name
        return provides.get(name)

    selected = []
    seen = set()
    queue = list(requested)
    while queue:
        name = queue.pop(0)
        real = lookup(name)
        if real is None:
            raise RuntimeError(f'Package {name} not found in any repository')
        if real in seen or real in excludes:
            continue
        seen.add(real)
        selected.append(real)
        fields = packages[real]
        for group in split_relations(fields.get('Pre-Depends', '')) + \
                split_relations(fields.get('Depends', '')):
            if any(alt in excludes for alt in group):
                continue
            choice = next((alt for alt in group if lookup(alt) in seen), None)
            if choice is None:
                choice = next((alt for alt in group if lookup(alt)), None)
            if choice is None:
                raise RuntimeError(f'Unresolvable dependency {" | ".join(group)} of {real}')
            queue.append(choice)
    return selected


def download_deb(fields, cache_dir):
    filename = fields['Filename']
    target = os.path.join(cache_dir, os.path.basename(filename))
    if os.path.exists(target):
        with open(target, 'rb') as f:
            if hashlib.sha256(f.read()).hexdigest() == fields['SHA256']:
                return target
    print(f' * Downloading {filename}', file=sys.stderr)
    data = fetch(f'{fields["Mirror"]}/{filename}')
    if hashlib.sha256(data).hexdigest() != fields['SHA256']:
        raise RuntimeError(f'SHA256 mismatch for {filename}')
    with open(target, 'wb') as f:
        f.write(data)
    return target


def relativize_symlinks(root):
    for dirpath, dirnames, filenames in os.walk(root):
        for name in dirnames + filenames:
            path = os.path.join(dirpath, name)
            if not os.path.islink(path):
                continue
            dest = os.readlink(path)
            if not dest.startswith('/'):
                continue
            new_dest = os.path.relpath(os.path.join(root, dest.lstrip('/')), dirpath)
            os.unlink(path)
            os.symlink(new_dest, path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', action='append', required=True,
                        help='"URL SUITE COMPONENT[,COMPONENT]", may be repeated')
    parser.add_argument('--arch', required=True, help='Debian architecture, e.g. amd64, armhf')
    parser.add_argument('--cache', required=True, help='Directory for downloaded .deb files')
    parser.add_argument('--output', required=True, help='Sysroot directory to create')
    parser.add_argument('--exclude', action='append', default=[],
                        help='Package to never install, may be repeated')
    parser.add_argument('--no-deps', action='store_true',
                        help='Install only the listed packages')
    parser.add_argument('packages', nargs='+')
    args = parser.parse_args()

    packages, provides = load_repos(args.repo, args.arch)
    excludes = DEFAULT_EXCLUDES | set(args.exclude)
    if args.no_deps:
        selected = args.packages
    else:
        selected = resolve(args.packages, packages, provides, excludes)

    os.makedirs(args.cache, exist_ok=True)
    if os.path.exists(args.output):
        shutil.rmtree(args.output)
    os.makedirs(args.output)

    manifest = []
    for name in sorted(selected):
        fields = packages[name]
        subprocess.check_call(['dpkg-deb', '-x', download_deb(fields, args.cache), args.output])
        manifest.append(f'{name}={fields["Version"]}')

    relativize_symlinks(args.output)
    for path in PRUNE:
        shutil.rmtree(os.path.join(args.output, path), ignore_errors=True)

    with open(os.path.join(args.output, 'sysroot.manifest'), 'w') as f:
        f.write(f'ARCH={args.arch}\n')
        f.write(''.join(f'REPO={repo}\n' for repo in args.repo))
        f.write('\n'.join(manifest) + '\n')


if __name__ == '__main__':
    main()
