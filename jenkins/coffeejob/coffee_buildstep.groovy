def RELEASE_TYPES = ['Debug', 'Release']

PROJECT_NAME = "Coffee"

LIN_UBNTU = "Ubuntu"
LIN_STMOS = "SteamOS"
LIN_RASPI = "Raspberry-Pi"
MAC_MCOSX = "Mac-OS-X"
WIN_WIN32 = "Windows"
WIN_MSUWP = "Windows-UWP"
LIN_ANDRD = "Android"

GEN_DOCS = "Docs"

A_X64 = "x86-64"
A_ARMV8A = "ARMv8A"
A_ARMV7A = "ARMv7A"
A_UNI = "Universal"

def LINUX_PACKAGING_OPTS = "-DCOFFEE_GENERATE_SNAPPY=ON"
def Targets = [
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
                   "raspi && native && test_platform"),
    /* Android on a Docker container, composite project
     */
    new BuildTarget(LIN_ANDRD, A_UNI,
                   "linux && docker && android",
                   null, null,
                   "Unix Makefiles", "",
                   "android && test_platform"),
    new BuildTarget(GEN_DOCS, A_UNI, "linux && docker",
                    "none_docs-none-none.cmake", "native-linux-generic.toolchain.cmake",
                    "Unix Makefiles", "", false, true),
]

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

void GetGithubKit(job, platform)
{
    job.with {
        wrappers {
            credentialsBinding {
                string("GH_API_TOKEN", "GithubToken")
            }
        }
    }
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

void GetDownstreamTrigger(job, downstream)
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

/* Setting up Git SCM
 * One function to change them all
 */
void GetSourceStep(descriptor, sourceDir, buildDir, job)
{
    def REPO_URL = 'https://github.com/hbirchtree/coffeecutie.git'

    GetGithubKit(job, descriptor.platformName)
    GetBuildParameters(job)

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

GH_RELEASE=`./github-cli --api-token $GH_API_TOKEN list tag hbirchtree/coffeecutie | sort -n | grep jenkins-auto | grep $GIT_COMMIT | sed -n 1p | cut -d '|' -f 2`

[ -z "${GH_RELEASE}" ] && exit 0
BUILD_NUMBER=`echo $GH_RELEASE | cut -d '-' -f 3`

echo ${GH_RELEASE} > ../GithubData.txt
echo ${BUILD_NUMBER} > ../GithubBuildNumber.txt

echo ${GH_RELEASE} > ../GithubData.txt
echo ${BUILD_NUMBER} > ../GithubBuildNumber.txt
'''
                )
            }
        }
    }
}

boolean IsDockerized(platName)
{
        return (platName == LIN_UBNTU || platName == LIN_STMOS
            || platName == LIN_RASPI || platName == LIN_ANDRD
            || platName == GEN_DOCS);
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
            scm {
                git {
                    remote {
                        name('origin')
                        url("https://github.com/hbirchtree/raspberry-sysroot.git")
                    }
                    extensions {
                        relativeTargetDirectory(raspi_sdk_dir)
                        cloneOptions {
                            shallow(true)
                        }
                    }
                }
            }
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

    job.with {
        /* Fixes some issues on Mac OS X with finding Brew */
        properties {
            environmentVariables {
                keepSystemVariables(true)
                keepBuildVariables(true)
            }
        }
    }

    if(IsDockerized(descriptor.platformName))
    {
        source_dir = "/source"
        build_dir = "/build"
        job.with {
            steps {
                shell(
                """
cd ${build_dir}
cmake ${source_dir} -G${descriptor.cmake_generator} -C${descriptor.cmake_preload} ${cmake_args} -DCMAKE_BUILD_TYPE=${variant}
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

    job.with {
        label(descriptor.label)
        scm {
            git {
                remote {
                    name("origin")
                    url(REPO_URL)
                }
                branch("master")
                extensions {
                    relativeTargetDirectory(meta_dir)
                    cloneOptions {
                        shallow(true)
                    }
                }
            }
        }
    }

    job.with {
        steps {
            shell(
              """
cd /home/coffee/build
cmake -G"Unix Makefiles" /home/coffee/project/android -DCMAKE_BUILD_TYPE=Debug -DSOURCE_DIR=/home/coffee/code -DANDROID_SDK=/home/coffee/android-sdk-linux -DANDROID_NDK=/home/coffee/android-ndk-linux
cmake --build /home/coffee/build
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
        GetGithubKit(job, descriptor.platformName)
        job.with {
            steps {
                shell(
                  '''
[ -z "${GH_API_TOKEN}" ] && exit 0
[ `lsb_release -r -s` = '14.04' ] && exit 0

[ ! -f "''' + buildDir + '''/GithubData.txt" ] && exit 0 # Exit if no Github data
GH_RELEASE=`cat ''' + buildDir + '''/GithubData.txt`
GH_BUILD_NUMBER=`cat ''' + buildDir + '''/GithubBuildNumber.txt`
tar -Jcvf ''' + releaseName + '''_$GH_BUILD_NUMBER.tar.xz ''' + artifact_glob + '''
./github-cli --api-token $GH_API_TOKEN push asset hbirchtree/coffeecutie:$GH_RELEASE ''' + releaseName + '''_$GH_BUILD_NUMBER.tar.xz
                  '''
                )
            }
        }
    }

    job.with {
        publishers {
            archiveArtifacts {
                pattern(artifact_glob)
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

def WORKSPACE = "/tmp"
def SOURCE_STEPS = []

for(t in Targets) {
    branch = "testing"
    pipelineName = "${PROJECT_NAME}_${t.platformName}_${t.platformArch}"
    sourceDir = "src"

    t.cmake_preload = "${sourceDir}/cmake/Preload/${t.cmake_preload}"
    t.cmake_toolchain = "${sourceDir}/cmake/Toolchains/${t.cmake_toolchain}"

    /* Create a pipeline per build target */
    pip = deliveryPipelineView("${pipelineName}")

    last_job = null
    i = 1

    for(rel in RELEASE_TYPES)
    {
        def releaseName = "${PROJECT_NAME}_${t.platformName}-${t.platformArch}"
        def binaryName = "binary_${t.platformName}-${t.platformArch}-${rel}"

        def job_name = "${i}.0_${pipelineName}"
        def pipeline_compile_name = "${rel} compilation stage"

        def workspaceDir = "${pipelineName}_${rel}"

        if(t.platformName == GEN_DOCS)
        {
            pipeline_compile_name = "Documentation generation"
        }else
        {
            job_name = job_name + "_${rel}"
        }

        def compile = job(job_name)
        def testing = null

        /* Compilation and testing will only be performed on suitable hosts */
        compile.with {
            customWorkspace(workspaceDir)
            deliveryPipelineConfiguration(pipelineName, pipeline_compile_name)
        }
        if(rel == "Release")
            compile.with {
                label(t.release_label)
            }
        else
            compile.with {
                label(t.label)
            }

        GetSourceStep(t, sourceDir, pipelineName, compile)
        if(i==1)
        {
            SOURCE_STEPS += compile
            pip.with {
                allowPipelineStart(true)
                showTotalBuildTime(true)
                pipelines {
                    component(pipelineName, compile.name)
                }
            }
        }

        if(t.do_tests)
        {
            testing = job("${i}.1_${pipelineName}_${rel}_Testing")

            GetBuildParameters(testing)

            testing.with {
                label(t.testing_label)
                customWorkspace(workspaceDir)
                deliveryPipelineConfiguration(pipelineName, "${rel} testing stage")
            }
        }

        GetJobQuirks(t, compile, testing, sourceDir)

        def buildDir = workspaceDir

        if(t.platformName == LIN_ANDRD)
        {
            GetCMakeMultiStep(t, compile, rel, 0, sourceDir, buildDir, "${WORKSPACE}/Coffee_Meta_src")
        }else{
            GetCMakeSteps(t, compile, rel, 0, sourceDir, buildDir)
            if(t.do_tests && t.testing_label != null)
                GetCMakeSteps(t, testing, rel, 1, sourceDir, buildDir)
        }

        GetDockerDataLinux(t, compile, sourceDir, buildDir, WORKSPACE, "${WORKSPACE}/Coffee_Meta_src")
        if(t.do_tests)
            GetDockerDataLinux(t, testing, sourceDir, buildDir, WORKSPACE, "${WORKSPACE}/Coffee_Meta_src")

        if(t.do_tests)
            GetArtifactingStep(testing, binaryName, workspaceDir, t)
        else
            GetArtifactingStep(compile, binaryName, workspaceDir, t)

        if(last_job != null)
                GetDownstreamTrigger(last_job, compile.name)
        if(testing != null)
            GetDownstreamTrigger(compile, testing.name)

        if(t.do_tests)
            last_job = testing
        else
            last_job = compile

        if(t.platformName == GEN_DOCS)
            break;

        /* Increment counter for ordering jobs in lists */
        i++;
    }
}

all_build_job = job('All Coffee')

GetBuildParameters(all_build_job)
GetGithubKit(all_build_job, LIN_UBNTU)
all_build_job.with {
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

SOURCE_STEPS.each {
    def src = it;
    GetDownstreamTrigger(all_build_job, src.name)
}
