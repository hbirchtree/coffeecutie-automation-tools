def RELEASE_TYPES = ['Debug', 'Release']

PROJECT_NAME = "Coffee"

LIN_UBNTU = "Ubuntu"
LIN_STMOS = "SteamOS"
LIN_RASPI = "Raspberry-Pi"
MAC_MCOSX = "Mac-OS-X"
WIN_WIN32 = "Windows"
WIN_MSUWP = "Windows-UWP"
LIN_ANDRD = "Android"

WEB_ASMJS = "Emscripten"

GEN_DOCS = "Docs"

A_X64 = "x86-64"
A_ARMV8A = "ARMv8A"
A_ARMV7A = "ARMv7A"
A_UNI = "Universal"
A_WEB = "Web"

BuildTarget[] GetTargets() {
    def LINUX_PACKAGING_OPTS = "-DCOFFEE_GENERATE_SNAPPY=ON"
    return [
    /* Creates Ubuntu binaries linked against the system
     * Also outputs AppImage, Snappy and Flatpak packages
     * for better cross-compatibility.
     */
    new BuildTarget(LIN_UBNTU, A_X64, "linux && docker && ubuntu && gcc && amd64",
                   "x86_64-linux-generic.cmake",
                   "native-linux-generic.toolchain.cmake",
                   "Ninja", LINUX_PACKAGING_OPTS + " -DSKIP_HIGHMEM_TESTS=ON"),
    /* SteamOS builds, use a special Docker environment */
    new BuildTarget(LIN_STMOS, A_X64, "linux && docker && steamos && gcc && amd64",
                   "x86_64-linux-steam.cmake",
                   "native-linux-generic.toolchain.cmake",
                   "Ninja", "", false),
    /* Creates Win32 applications, bog standard,
     * self-contained resources.
     */
    new BuildTarget(WIN_WIN32, A_X64, "windows && vcpp && x64",
                   "x86_64-windows-generic.cmake",
                   "x86_64-windows-win32.toolchain.cmake",
                   "Visual Studio 14 2015 Win64", "-DSKIP_HIGHMEM_TESTS=ON"),
    /* Windows UWP produces AppX directories for containment */
    new BuildTarget(WIN_MSUWP, A_X64, "windows && win10sdk && x64",
                   "x86_64-windows-uwp.cmake",
                   "x86_64-windows-uwp.toolchain.cmake",
                   "Visual Studio 14 2015 Win64", "", false),
    /* Good old OS X .app directories with some spice,
     * self-contained resources.
     */
    new BuildTarget(MAC_MCOSX, A_X64, "macintosh && clang && x64",
                   "x86_64-osx-generic.cmake",
                   "native-macintosh-generic.toolchain.cmake",
                   "Ninja", "-DSKIP_HIGHMEM_TESTS=ON"),
    /* Raspberry Pi, using a Docker container
     * Will require a special docker-compose for simplicity with volumes
     */
    new BuildTarget(LIN_RASPI, A_UNI, "linux && docker",
                  "raspberry.cmake",
                  "gnueabihf-arm-raspberry.toolchain.cmake",
                   "Ninja", "-DRASPBERRY_SDK=/raspi-sdk",
                   /*"raspi && native && test_platform"*/, false),
    /* Android on a Docker container, composite project
     */
    new BuildTarget(LIN_ANDRD, A_UNI,
                   "linux && docker && android",
                   null, null,
                   "Unix Makefiles", "",
                   /*"android && test_platform"*/, false),
    new BuildTarget(GEN_DOCS, A_UNI, "linux && docker",
                    "none_docs-none-none.cmake", "native-linux-generic.toolchain.cmake",
                    "Unix Makefiles", "", false, true),
    new BuildTarget(WEB_ASMJS, A_WEB, "linux && docker",
                    "emscripten.cmake", "js-emscripten.toolchain.cmake",
                    "Ninja", "-DNATIVE_LIB_ROOT=nativelib -DEMSCRIPTEN_ROOT_PATH=/emsdk_portable/emscripten/master", false)
        ]
}

class BuildTarget
{
    BuildTarget(String platName, String platArch,
                String label, String cPreload,
                String cTC, String cGen, String cOpts)
    {
        platformName = platName;
        platformArch = platArch;
        this.label = label;
        this.release_label = label;
        this.testing_label = label;
        cmake_preload = cPreload;
        cmake_toolchain = cTC;
        cmake_generator = cGen;
        cmake_options = cOpts;

        do_tests = true;
        is_documentation = false;
    }

    BuildTarget(String platName, String platArch,
                String label, String cPreload,
                String cTC, String cGen, String cOpts,
                boolean do_tests)
    {
        platformName = platName;
        platformArch = platArch;
        this.label = label;
        this.release_label = label;
        this.testing_label = label;
        cmake_preload = cPreload;
        cmake_toolchain = cTC;
        cmake_generator = cGen;
        cmake_options = cOpts;

        this.do_tests = do_tests;
        is_documentation = false;
    }

    BuildTarget(String platName, String platArch,
                String build_label, String cPreload,
                String cTC, String cGen, String cOpts,
                String test_label)
    {
        platformName = platName;
        platformArch = platArch;
        label = build_label;
        this.release_label = label;
        testing_label = test_label;
        cmake_preload = cPreload;
        cmake_toolchain = cTC;
        cmake_generator = cGen;
        cmake_options = cOpts;

        this.do_tests = true;
        is_documentation = false;
    }

    BuildTarget(String platName, String platArch,
                String label, String cPreload,
                String cTC, String cGen, String cOpts,
                boolean do_tests, boolean docs)
    {
        platformName = platName;
        platformArch = platArch;
        this.label = label;
        this.release_label = label;
        this.testing_label = label;
        cmake_preload = cPreload;
        cmake_toolchain = cTC;
        cmake_generator = cGen;
        cmake_options = cOpts;

        this.do_tests = do_tests;
        is_documentation = docs;
    }

    String platformName;
    String platformArch;
    String label;
    String testing_label;
    String release_label;

    String cmake_preload;
    String cmake_toolchain;
    String cmake_generator;
    String cmake_options;

    boolean do_tests;
    boolean is_documentation;
};

void GetBuildParameters(job)
{
    job.with {
        parameters {
            stringParam('GH_BRANCH', "testing", 'Name of source Github ref')
            stringParam('GH_RELEASE', 'jenkins-auto-${BUILD_NUMBER}', 'Name of the generated Github release')
        }
    }
}

void GetGithubToken(job)
{
    job.with {
        wrappers {
            credentialsBinding {
                string("GH_API_TOKEN", "GithubToken")
            }
        }
    }
}

void GetGithubKit(job, platform)
{
    if(platform != WIN_WIN32 && platform != WIN_MSUWP)
    {
        job.with {
            steps {
                shell(
                '''
set +x
KERN=`uname`
case $KERN in
"Linux")
[ `lsb_release -r -s` = "14.04" ] && exit 0 # For older Docker images
wget -q https://github.com/hbirchtree/qthub/releases/download/v1.0.1.1/github-cli -O github-cli
;;
"Darwin")
# On OS X, we do not know if wget is in PATH. We'll check up with BASH
`bash -c 'which wget'` -q https://github.com/hbirchtree/qthub/releases/download/v1.0.1.1/github-cli-osx -O github-cli
;;
*)
exit 0
;;
esac
chmod +x github-cli
'''
                )
            }
        }
    }
}

void GetDownstreamTrigger(job, downstream, fail)
{
    job.with {
        steps {
            downstreamParameterized {
                trigger(downstream) {
                    parameters {
                        currentBuild()
                    }
                }
            }
        }
    }
}

void GetGHStatusTransmitter(job, desc, end)
{
    if(job == null || desc.platformName == WIN_WIN32 || desc.platformName == WIN_MSUWP)
        return

    def code = desc.platformName
    def codeLower = code.toLowerCase()
    def p1 = """

cd src
BUILD_VARIANT=${code}
GIT_SHA=`git rev-parse HEAD`
""" +
    '''
curl -X POST -H "Authorization: token $GH_API_TOKEN" https://api.github.com/repos/hbirchtree/coffeecutie/statuses/$GIT_SHA -d "{\\"state\\": \\"$BUILD_STATE\\", \\"description\\": \\"$BUILD_VARIANT build\\", \\"context\\": \\"continuous-integration/jenkins/$BUILD_VARIANT\\"}" '''
    if(end)
    {
        job.with {
            publishers {
                postBuildScripts {
                    onlyIfBuildSucceeds(true)
                    steps {
                        shell("BUILD_STATE=success" + p1)
                    }
                }
            }
        }
    }
    job.with {
        publishers {
            postBuildScripts {
                onlyIfBuildSucceeds(false)
                onlyIfBuildFails(true)
                steps {
                    shell("BUILD_STATE=failure" + p1)
                }
            }
        }
    }
}

void GetBuildNumber(descriptor, sourceDir, job)
{
    if(descriptor.platformName != WIN_WIN32 && descriptor.platformName != WIN_MSUWP)
    {
        job.with {
            steps {
                shell(
                  '''
[ -z "${GH_API_TOKEN}" ] && exit 0
[ `lsb_release -r -s` = '14.04' ] && exit 0

cd ''' + sourceDir + '''
GIT_COMMIT=`git rev-parse HEAD`

GH_RELEASE=`../github-cli --api-token $GH_API_TOKEN list tag hbirchtree/coffeecutie | sort -n | grep jenkins-auto | grep $GIT_COMMIT | sed -n 1p | cut -d '|' -f 2`

[ -z "${GH_RELEASE}" ] && exit 0
BUILD_NUMBER=`echo $GH_RELEASE | cut -d '-' -f 3`

echo ${GH_RELEASE} > ../GithubData.txt
echo ${BUILD_NUMBER} > ../GithubBuildNumber.txt
'''
                )
            }
        }
    }
}

/* Setting up Git SCM
 * One function to change them all
 */
void GetSourceStep(descriptor, sourceDir, buildDir, job)
{
    def REPO_URL = 'https://github.com/hbirchtree/coffeecutie.git'

    job.with {
        label(descriptor.label)
        scm {
            git {
                remote {
                    name('origin')
                    url(REPO_URL)
                }
                branch('${GH_BRANCH}')
                extensions {
                    relativeTargetDirectory(sourceDir)
                    submoduleOptions {
                        recursive(true)
                    }
                    cloneOptions {
                        shallow(true)
                    }
                }
            }
        }
    }

    GetBuildNumber(descriptor, sourceDir, job)
}

void GetExtraSourceSteps(platformName, j)
{
    def RepoUrl = null
    def SubdirPath = null

    if(platformName == LIN_ANDRD)
    {
        SubdirPath = 'meta_src'
        RepoUrl = 'https://github.com/hbirchtree/coffeecutie-meta.git'
    }else if(platformName == LIN_RASPI)
    {
        SubdirPath = 'raspi-sdk'
        RepoUrl = 'https://github.com/hbirchtree/raspberry-sysroot.git'
    }else if(platformName == WEB_ASMJS)
    {
        SubdirPath = 'nativelib'
        RepoUrl = 'https://github.com/hbirchtree/native-library-bundle.git'
    }

    if(RepoUrl == null || SubdirPath == null)
        return;

    j.with {
        scm {
            git {
                remote {
                    name('origin')
                    url(RepoUrl)
                }
                branch('master')
                extensions {
                    relativeTargetDirectory(SubdirPath)
                    submoduleOptions {
                        recursive(true)
                    }
                    cloneOptions {
                        shallow(true)
                    }
                }
            }
        }
    }
}

boolean IsDockerized(platName)
{
        return (platName == LIN_UBNTU || platName == LIN_STMOS
            || platName == LIN_RASPI || platName == LIN_ANDRD
            || platName == GEN_DOCS || platName == WEB_ASMJS);
}

String GetAutomationDir(sourceDir)
{
    return "${sourceDir}/tools/automation/"
}

String GetDockerBuilder(variant)
{
    return "builders/${variant}"
}

void GetDockerDataLinux(descriptor, job, sourceDir, buildDir, workspaceRoot, meta_dir)
{
    def docker_dir = 'ubuntu';
    def docker_file = "Dockerfile";

    /* Typical Linux build environments */
    if(descriptor.platformName == LIN_UBNTU)
    {}
    else if(descriptor.platformName == LIN_STMOS)
        docker_dir = "steam"
    else if(descriptor.platformName == LIN_ANDRD)
    {
        job.with {
            wrappers {
                buildInDocker {
                    dockerfile(GetAutomationDir(sourceDir)+GetDockerBuilder("android"), "Dockerfile")
                    verbose()
                    volume(buildDir, "/home/coffee/build")
                    volume(sourceDir, "/home/coffee/code")
                    volume(meta_dir, "/home/coffee/project")
                }
            }
        }
        return;
    }
    else if(descriptor.platformName == LIN_RASPI)
    {
        /* For Raspberry Pi, we need to download a sysroot
         * The sysroot contains libraries and headers
         * We download it from Github, but we should replace
         * it with a better distribution method
         */
        def raspi_sdk_dir = "${workspaceRoot}/raspi-sdk"
        def raspi_sdk = "/raspi-sdk"

        job.with {
            wrappers {
                buildInDocker {
                    dockerfile(GetAutomationDir(sourceDir)+GetDockerBuilder("raspberry"), "Dockerfile")
                    verbose()
                    volume(buildDir, "/build")
                    volume(sourceDir, "/source")
                    volume(raspi_sdk_dir + "/architectures/rpi-SDL2-X11-armv7a",raspi_sdk)
                }
            }
        }
        return;
    }else if(descriptor.platformName == WEB_ASMJS)
    {
        docker_dir = "emscripten"
        job.with {
            steps {
                environmentVariables {
                    env('EMSCRIPTEN', '/emsdk_portable')
                }
            }
        }
    }else if(descriptor.platformName == GEN_DOCS)
        docker_dir = "doc-generator"
    else
        return;

    /* Normally, we just use a stock Docker container without many extras */
    docker_dir = GetAutomationDir(sourceDir)+GetDockerBuilder(docker_dir)

    job.with {
        wrappers {
            buildInDocker {
                dockerfile(docker_dir, docker_file)
                verbose()
                volume(buildDir, "/build")
                volume(sourceDir, "/source")
            }
        }
    }
}

void GetCMakeSteps(descriptor, job, variant, level, source_dir, build_dir)
{
    cmake_args = "-DCMAKE_TOOLCHAIN_FILE=${descriptor.cmake_toolchain} -DCMAKE_INSTALL_PREFIX=out "
    cmake_args += descriptor.cmake_options

    cmake_target = "install"

    if(level == 1)
    {
        if(descriptor.platformName == WIN_WIN32
           || descriptor.platformName == WIN_MSUWP)
        {
            /* Runs MSBuild testing? */
            cmake_target = "RUN_TESTS"
        }else{
            /* Runs CTest */
            cmake_target = "test"
        }
    }

    if(descriptor.platformName == MAC_MCOSX)
    {
        job.with {
            /* Fixes some issues on Mac OS X with finding Brew */
            properties {
                environmentVariables {
                    keepSystemVariables(true)
                    keepBuildVariables(true)
                }
            }
        }
    }

    if(IsDockerized(descriptor.platformName))
    {
        source_dir = "/source"
        build_dir = "/build"
    }
    if(descriptor.platformName == MAC_MCOSX)
    {
        job.with {
            steps {
                environmentVariables {
                    env('CC', 'clang-3.8')
                    env('CXX', 'clang++-3.8')
                }
            }
        }
    }
    if(IsDockerized(descriptor.platformName) || descriptor.platformName == MAC_MCOSX)
    {
        job.with {
            steps {
                shell(
                """
cd ${build_dir}
cmake ${source_dir} -G'${descriptor.cmake_generator}' -C${descriptor.cmake_preload} ${cmake_args} -DCMAKE_BUILD_TYPE=${variant}
cmake --build ${build_dir} --target ${cmake_target}
""")
            }
        }
    }else{
        job.with {
            steps {
                cmake {
                    generator(descriptor.cmake_generator)
                    args(cmake_args)
                    preloadScript(descriptor.cmake_preload)
                    sourceDir(source_dir)
                    buildDir(build_dir)
                    buildType(variant)

                    buildToolStep {
                        useCmake(true)
                        args("--target ${cmake_target}")
                    }
                }
            }
        }
    }
}

/* For special CMake jobs
 * Eg. multi-arch Android builds, multi-arch Ubuntu builds
 */
void GetCMakeMultiStep(descriptor, job, variant, level, source_dir, build_dir, meta_dir)
{
    def REPO_URL = 'https://github.com/hbirchtree/coffeecutie-meta.git'

    def isoBuild = "/home/coffee/build"
    def isoCode = "/home/coffee/code"
    def isoProj = "/home/coffee/project"

    job.with {
        label(descriptor.label)
    }

    job.with {
        steps {
            shell(
              """
cd ${isoBuild}
cmake -G"Unix Makefiles" ${isoProj}/android -DCMAKE_BUILD_TYPE=Debug -DSOURCE_DIR=${isoCode} -DANDROID_SDK=/home/coffee/android-sdk-linux -DANDROID_NDK=/home/coffee/android-ndk-linux
cmake --build ${isoBuild}
              """
            )
        }
    }
}

/* Emscripten was so bad to set up that I just put this here.
 * Something was really weird about Jenkins' way of inputting environment variables.
 */
void GetEmscriptenStep(descriptor, job, variant, target)
{
    job.with {
        steps {
            shell(
            '''
docker run --rm --workdir /build -v ${WORKSPACE}:/build emscripten:v2 \
    cmake /build/src -GNinja \
        -DNATIVE_LIB_ROOT=nativelib \
        -DEMSCRIPTEN_ROOT_PATH=/emsdk_portable/emscripten/master \
        ''' + """-Csrc/cmake/Preload/${descriptor.cmake_preload} \
        -DCMAKE_TOOLCHAIN_FILE=src/cmake/Toolchains/${descriptor.cmake_toolchain} \
        -DCMAKE_INSTALL_PREFIX=out \
        -DCMAKE_BUILD_TYPE=${variant}""" + '''
docker run --rm --workdir /build -v ${WORKSPACE}:/build emscripten:v2 \
    ''' + """cmake --build /build --target ${target}
"""
            )
        }
    }
}

void GetArtifactingStep(job, releaseName, buildDir, descriptor)
{
    def artifact_glob = "out/**"

    if(descriptor.platformName == GEN_DOCS)
    {
        artifact_glob = "out/docs/html/**"
    }

    if(descriptor.platformName != WIN_WIN32 && descriptor.platformName != WIN_MSUWP)
    {
        job.with {
            steps {
                shell(
                  '''
[ -z "${GH_API_TOKEN}" ] && exit 0
[ `lsb_release -r -s` = '14.04' ] && exit 0

[ ! -f "GithubData.txt" ] && exit 0 # Exit if no Github data
GH_RELEASE=`cat GithubData.txt`
GH_BUILD_NUMBER=`cat GithubBuildNumber.txt`
tar -Jcvf ''' + releaseName + '''_$GH_BUILD_NUMBER.tar.xz ''' + artifact_glob + '''
./github-cli --api-token $GH_API_TOKEN push asset hbirchtree/coffeecutie:$GH_RELEASE ''' + releaseName + '''_$GH_BUILD_NUMBER.tar.xz
                  '''
                )
            }
        }
    }else{
        job.with {
            publishers {
                archiveArtifacts {
                    pattern(artifact_glob)
                }
            }
        }
    }
}

/* Adds platform-specific features to jobs
 * Useful for fixing issues
 */
void GetJobQuirks(descriptor, compile, testing, sourceDir)
{
    if(descriptor.platformName == MAC_MCOSX)
    {
        compile.with {
            steps {
                shell(
                  """
cd "${sourceDir}/desktop/osx/"
bash "gen_icons.sh"
""")
            }
        }
    }
}

class BuildInstance
{
    String pipeline;
    String binaryName;
    String workspace;
    String sourceDir;
    String buildDir;
    String mode;
    String compileLabel;
};

class PipelineDescriptor
{
    def pipeline;
    def lastStep;
};

def GetBasePipeline(project, desc)
{
    def base = deliveryPipelineView("${project}_${desc.platformName}_${desc.platformArch}")
    base.with {
        allowPipelineStart(true)
        showTotalBuildTime(true)
    }
    def pip = new PipelineDescriptor()
    pip.pipeline = base
    return pip
}

def GetBaseJob(name, workspace)
{
    def j = job(name)
    GetBuildParameters(j)
    GetGithubToken(j)
    j.with {
        customWorkspace(workspace)
    }
    return j
}

def ChainJobWithPipeline(pipeline, job, label)
{
    if(pipeline.lastStep == null)
    {
        pipeline.pipeline.with {
            pipelines {
                component(pipeline.pipeline.name,job.name)
            }
        }
    }else{
        GetDownstreamTrigger(pipeline.lastStep, job.name, false)
    }
    pipeline.lastStep = job
    job.with {
        deliveryPipelineConfiguration(pipeline.pipeline.name, label)
    }
}

def GetCompileJob(desc, mode, workspace)
{
    def sourceSubDir = "src"
    mode.buildDir = '${WORKSPACE}/'
    mode.sourceDir = mode.buildDir + sourceSubDir
    def metaDir = mode.buildDir + "meta_src"

    def base = GetBaseJob("Compile_${desc.platformName}_${desc.platformArch}",
                          workspace)
    GetGithubKit(base, desc.platformName)
    GetSourceStep(desc, sourceSubDir, mode.pipeline, base)
    if(mode.mode != "Release")
    {
        base.with {
            label(desc.label)
        }
        mode.compileLabel = desc.release_label
    }
    else
    {
        base.with {
            label(desc.release_label)
        }
        mode.compileLabel = desc.release_label
    }
    if(desc.platformName != WEB_ASMJS)
    {
        GetDockerDataLinux(desc, base, mode.sourceDir, mode.buildDir,
                           mode.buildDir, metaDir)
        if(desc.platformName == LIN_ANDRD)
            GetCMakeMultiStep(desc, base, mode.mode, 0, mode.sourceDir,
                              mode.buildDir, metaDir)
        else
            GetCMakeSteps(desc, base, mode.mode, 0, mode.sourceDir, mode.buildDir)
    }else{
        /* Emscripten build does not work with Jenkins' Docker support
         * We need a better alternative.
         */
        GetEmscriptenStep(desc, base, mode.mode, "install")
    }

    return base
}

def GetTestingJob(desc, mode, workspace)
{
    /* Directory for meta-project */
    def metaDir = mode.buildDir + "meta_src"

    def base = GetBaseJob("Test_${desc.platformName}_${desc.platformArch}",
                          workspace)
    base.with {
        label(desc.testing_label)
    }
    GetDockerDataLinux(desc, base, mode.sourceDir, mode.buildDir,
                       mode.buildDir, metaDir)
    /* Check if testing happens on build machine */
    if(desc.testing_label == desc.label)
        GetCMakeSteps(desc, base, mode.mode, 1, mode.sourceDir, mode.buildDir)
    return base
}

def GetCompileTestingPair(pip, desc, mode, workspace)
{
    def exsource = null

    def compile = GetCompileJob(desc, mode, workspace)
    def last_job = compile
    def testing = null
    def deploy = null
    if(desc.do_tests)
    {
        testing = GetTestingJob(desc, mode, workspace)
        last_job = testing
    }
    GetJobQuirks(desc, compile, testing, mode.sourceDir)

    if(desc.platformName != GEN_DOCS)
        exsource = GetBaseJob("ExtraSource_${desc.platformName}_${desc.platformArch}",
                              workspace)
    if(exsource != null)
    {
        GetExtraSourceSteps(desc.platformName, exsource)
        exsource.with {
            label(mode.compileLabel)
        }
    }

    def artifact_step = testing
    if((desc.testing_label != desc.label
        && mode.mode == "Debug")
       || (desc.testing_label != desc.release_label
           && mode.mode == "Release") || testing == null)
        artifact_step = compile
    GetArtifactingStep(artifact_step, mode.binaryName,
                       mode.workspace, desc)

    if(desc.platformName != GEN_DOCS)
    {
        if(exsource != null)
            exsource.name = "0_" + exsource.name + "_${mode.mode}"
        compile.name = "1_" + compile.name + "_${mode.mode}"
        if(testing != null)
            testing.name = "2_" + testing.name + "_${mode.mode}"
    }

    /* Big problem: SteamOS building happens in a Trusty container,
     *  which does not support recent enough Qt. Solution: Another step, unisolated.
     * It's terrible, but it works.
     */
    if(desc.platformName == LIN_STMOS)
    {
        deploy = GetBaseJob("3_Deploy_${desc.platformName}_${desc.platformArch}_${mode.mode}",
                                workspace)
        last_job = deploy
        GetGithubKit(deploy, LIN_UBNTU)
        GetBuildNumber(desc, "src", deploy)
        GetArtifactingStep(deploy, mode.binaryName,
                           mode.workspace, desc)
        deploy.with {
            label(mode.compileLabel)
        }
    }

    if(mode.mode == "Debug")
    {
        if(exsource != null)
            exsource.name = "1." + exsource.name
        compile.name = "1." + compile.name
        if(testing != null)
                testing.name = "1." + testing.name
        if(deploy != null)
            deploy.name = "1." + deploy.name
    }else{
        if(exsource != null)
            exsource.name = "2." + exsource.name
        compile.name = "2." + compile.name
        if(testing != null)
            testing.name = "2." + testing.name
        if(deploy != null)
            deploy.name = "2." + deploy.name
    }

    if(exsource != null)
    {
        ChainJobWithPipeline(pip, exsource, "Pre-source ${desc.platformName}_${desc.platformArch}_${mode.mode}")
    }
    ChainJobWithPipeline(pip, compile,
                         "Compile ${desc.platformName}_${desc.platformArch}_${mode.mode}")
    if(testing != null)
    {
        ChainJobWithPipeline(pip, testing,
                             "Test ${desc.platformName}_${desc.platformArch}_${mode.mode}")
    }
    if(deploy != null)
    {
        ChainJobWithPipeline(pip, deploy,
                             "Deploy ${desc.platformName}_${desc.platformArch}_${mode.mode}")
    }

    def pre_list = [exsource, compile, testing, deploy]
    def post_list = []
    pre_list.each {
        if(it != null)
                post_list += it
    }
    return post_list
}

def GetPipeline(project, target, view_data)
{
    def inst_dbg = new BuildInstance();
    def inst_rel = new BuildInstance();
    def source_step = null

    def base = GetBasePipeline(project, target)

    inst_dbg.pipeline = base.pipeline.name
    inst_rel.pipeline = base.pipeline.name
    inst_dbg.binaryName = "binary_${target.platformName}_${target.platformArch}_Dbg"
    inst_rel.binaryName = "binary_${target.platformName}_${target.platformArch}_Rel"
    inst_dbg.workspace = "${target.platformName}_${target.platformArch}_Dbg"
    inst_rel.workspace = "${target.platformName}_${target.platformArch}_Rel"
    inst_dbg.mode = "Debug"
    inst_rel.mode = "Release"

    if(target.platformName == GEN_DOCS)
    {
        inst_dbg.binaryName = "Documentation"
    }

    def debug_pair = GetCompileTestingPair(base, target, inst_dbg, inst_dbg.workspace)
    def release_pair = null
    def final_job = debug_pair.last()
    if(target.platformName != GEN_DOCS)
    {
        release_pair = GetCompileTestingPair(base, target, inst_rel, inst_dbg.workspace)
        final_job = release_pair.last()
    }

    debug_pair.each {
        def stat = (it == debug_pair.last() && release_pair == null)
        GetGHStatusTransmitter(it, target, stat)
    }
    if(release_pair != null)
        release_pair.each {
            def stat = (it == release_pair.last())
            GetGHStatusTransmitter(it, target, stat)
        }

    if(target.platformName == GEN_DOCS)
        debug_pair[0].name = "1.0_Compile_Documentation"

    view_data[0].with {
        views {
            deliveryPipelineView(base.pipeline.name)
        }
    }

    return debug_pair[0]
}

def GetAllView()
{
    def base = nestedView("Coffee")
    def meta = deliveryPipelineView("Meta")
    base.with {
        views {
            deliveryPipelineView(meta.name)
        }
    }
    return [base, meta]
}

def view_data = GetAllView()
def SOURCE_STEPS = []

for(t in GetTargets()) {
    if(t.platformName != WEB_ASMJS)
    {
        t.cmake_preload = '${WORKSPACE}' + "/src/cmake/Preload/${t.cmake_preload}"
        t.cmake_toolchain = '${WORKSPACE}' + "/src/cmake/Toolchains/${t.cmake_toolchain}"
    }

    SOURCE_STEPS += GetPipeline('Coffee', t, view_data)
}

def GetAllJob(source_steps, meta)
{
    def base = job('All Coffee')
    GetBuildParameters(base)
    GetGithubToken(base)
    GetGithubKit(base, LIN_UBNTU)
    base.with {
        label('ubuntu && amd64')
        steps {
            shell(
            '''
[ -z "${GH_API_TOKEN}" ] && exit 0
[ `lsb_release -r -s` = '14.04' ] && exit 0
./github-cli --api-token $GH_API_TOKEN push tag hbirchtree/coffeecutie:$GH_BRANCH $GH_RELEASE
./github-cli --api-token $GH_API_TOKEN push release hbirchtree/coffeecutie:$GH_RELEASE "Jenkins Auto Build $BUILD_NUMBER"
'''
            )
        }
    }
    source_steps.each {
        GetDownstreamTrigger(base, it.name, false)
    }
    meta.with {
        pipelines {
            component('Compile all Coffee', base.name)
        }
    }
}

GetAllJob(SOURCE_STEPS, view_data[1])
