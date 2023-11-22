#!/usr/bin/env python3

import json
from glob import glob
from os.path import dirname


def manifest_to_path(source):
    if source.endswith('compiler.manifest'):
        return None
    with open(source, 'r') as manifest_file:
        name = source.split('/')[2].split('.')[0].split('-')
        platform = source.split('/')[-2].replace('-ct', '')
        if name[0] == platform:
            architecture = '-'.join(name[1:])
        else:
            architecture = '-'.join(name)
        related_files = glob(f'{"/".join(source.split("/")[:-1])}/{platform}-{architecture}*.tar.xz')
        if len(related_files) == 0:
            return None
        manifest = manifest_file.readlines()
        manifest = [line.replace('\n', '') for line in manifest]
        manifest = [line for line in manifest if line != '']
        manifest = [line.replace('"', '').split('=') for line in manifest]
        manifest = {prop[0]: prop[1] for prop in manifest}
        if 'CT_GLIBC_VERSION' in manifest:
            dist_name = f'{platform}+{architecture}+gcc{manifest["CT_GCC_VERSION"]}-glibc{manifest["CT_GLIBC_VERSION"]}'
        elif 'CT_NEWLIB_VERSION' in manifest:
            dist_name = f'{platform}+{architecture}+gcc{manifest["CT_GCC_VERSION"]}-newlib{manifest["CT_NEWLIB_VERSION"]}'
        else:
            dist_name = f'{platform}+{architecture}+gcc{manifest["CT_GCC_VERSION"]}'
        return dist_name, [source] + related_files


def assemble_manifest(root_dir):
    manifests = glob(f'{root_dir}/*-ct/*.manifest')
    manifests = [manifest_to_path(manifest) for manifest in manifests]
    manifests = [manifest for manifest in manifests if manifest is not None]
    manifests = [{
            "name": '-'.join(manifest[0].split('+')[:2]),
            "platform": manifest[0].split('+')[0],
            "architecture": manifest[0].split('+')[1],
            "toolchain": manifest[0],
            "manifest": manifest[1][0],
            "files": manifest[1][1:]
        } for manifest in manifests]
    for manifest in manifests:
        manifest['mapping'] = {}
        manifest['sysroot'] = [file.split('/')[-1].split('.')[0].split('_')[-1] for file in manifest['files'] if '_target.' not in file][0]
        if manifest['sysroot'] == 'compiler':
            del manifest['sysroot']
        toolchain = manifest['toolchain'].replace('+', '_')
        for file in manifest['files'] + [manifest['manifest']]:
            ext = '.'.join(file.split('/')[-1].split('.')[1:])
            compiler_parts = file.split('/')[-1].split('.')[0].split('+')
            if len(compiler_parts) == 4:
                arch, sysroot, _, _ = compiler_parts
            elif len(compiler_parts) == 3:
                arch, sysroot, _ = compiler_parts
            elif len(compiler_parts) == 2:
                arch, sysroot = compiler_parts
                # Compiler-only toolchain, no sysroot
                if sysroot == 'compiler':
                    sysroot = None
            else:
                arch = compiler_parts[0]
                sysroot = None
            if '_target.' in file:
                manifest['mapping'][file] = f'{toolchain}_{manifest["sysroot"]}_target.{ext}'
                manifest['targetfs'] = f'{toolchain}_{manifest["sysroot"]}_target.{ext}'
            elif file.endswith('.manifest'):
                manifest['mapping'][file] = f'{toolchain}.{ext}'
            elif sysroot is not None:
                manifest['mapping'][file] = f'{toolchain}_{sysroot}.{ext}'
                manifest['compiler'] = f'{toolchain}_{sysroot}.{ext}'
            elif 'sysroot' in manifest:
                manifest['mapping'][file] = f'{toolchain}_{manifest["sysroot"]}.{ext}'
                manifest['compiler'] = f'{toolchain}_{manifest["sysroot"]}.{ext}'
            elif sysroot is None:
                manifest['mapping'][file] = f'{toolchain}_bare.{ext}'
                manifest['compiler'] = f'{toolchain}_bare.{ext}'
        manifest['toolchain'] = toolchain
    out = {
        'sources': manifests,
        'outputs': [manifest.copy() for manifest in manifests],
    }
    for manifest in out['outputs']:
        manifest['files'] = [manifest['mapping'][key].split('/')[-1] for key in manifest['mapping']]
        manifest['manifest'] = f'{manifest["toolchain"]}.manifest'
        del manifest['mapping']

    print(json.dumps(out))


def main():
    assemble_manifest('.')


if __name__ == '__main__':
    main()
