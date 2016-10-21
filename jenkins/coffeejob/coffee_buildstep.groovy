def RELEASE_TYPES = ['Debug', 'Release']

PROJECT_NAME = "Coffee"

LIN_UBNTU = "Ubuntu"
LIN_STMOS = "SteamOS"
LIN_RASPI = "Raspberry-Pi"
MAC_MCOSX = "Mac-OS-X"
WIN_WIN32 = "Windows"
WIN_MSUWP = "Windows-UWP"
LIN_ANDRD = "Android"

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
                   "Ninja", LINUX_PACKAGING_OPTS),
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
                   "Visual Studio 14 2015 Win64", ""),
    /* Windows UWP produces AppX directories for containment */
    new BuildTarget(WIN_MSUWP, A_X64, "windows && win10sdk && x64",
                   "x86_64-windows-uwp.cmake",
                   "x86_64-windows-uwp.toolchain.cmake",
                   "Visual Studio 14 2015 Win64", ""),
    /* Good old OS X .app directories with some spice,
     * self-contained resources.
     */
    new BuildTarget(MAC_MCOSX, A_X64, "macintosh && clang && x64",
                   "x86_64-osx-generic.cmake",
                   "native-macintosh-generic.toolchain.cmake",
                   "Ninja", ""),
    /* Raspberry Pi, using a Docker container
     * Will require a special docker-compose for simplicity with volumes
     */
    new BuildTarget(LIN_RASPI, A_UNI, "linux && docker && raspi && bcm_gcc && armv7a",
                  "raspberry.cmake",
                  "gnueabihf-arm-raspberry.toolchain.cmake",
                                  "Ninja", "-DRASPBERRY_SDK=/raspi-sdk"),
    /* Android on a Docker container, composite project
     */
    new BuildTarget(LIN_ANDRD, A_UNI,
                   "linux && docker && android && android_sdk && android_ndk",
                   "android.cmake",
                   "all-android.toolchain.cmake",
                                   "Ninja", "")
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
        cmake_preload = cPreload;
        cmake_toolchain = cTC;
        cmake_generator = cGen;
        cmake_options = cOpts;

	do_tests = true;
    }

    BuildTarget(String platName, String platArch,
		String label, String cPreload,
		String cTC, String cGen, String cOpts,
		boolean do_tests)
    {
	platformName = platName;
	platformArch = platArch;
	this.label = label;
	cmake_preload = cPreload;
	cmake_toolchain = cTC;
	cmake_generator = cGen;
	cmake_options = cOpts;

	this.do_tests = do_tests;
    }

    String platformName;
    String platformArch;
    String label;

    String cmake_preload;
    String cmake_toolchain;
    String cmake_generator;
    String cmake_options;

    boolean do_tests;
};

/* Setting up Git SCM
 * One function to change them all
 */
void GetSourceStep(descriptor, sourceDir, job, branch_)
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
                branch(branch_)
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
}

boolean IsDockerized(platName)
{
	return (platName == LIN_UBNTU || platName == LIN_STMOS
            || platName == LIN_RASPI || platName == LIN_ANDRD);
}

String GetAutomationDir(sourceDir)
{
    return "${sourceDir}/tools/automation/"
}

String GetDockerBuilder(variant)
{
    return "builders/${variant}"
}

void GetDockerDataLinux(descriptor, job, sourceDir, buildDir)
{
    def docker_dir = 'ubuntu';
    def docker_file = "Dockerfile";

    /* Typical Linux build environments */
    if(descriptor.platformName == LIN_UBNTU)
    {}
    else if(descriptor.platformName == LIN_STMOS)
        docker_dir = "steam"
    else
        return;

    docker_dir = GetAutomationDir(sourceDir)+GetDockerBuilder(docker_dir)

    job.with {
        wrappers {
            buildInDocker {
                dockerfile(docker_dir, docker_file)
                verbose()
                volume(buildDir, buildDir)
                volume(sourceDir, sourceDir)
            }
        }
    }
}

void GetDockerDataRaspberry(descriptor, job)
{
    if(job.platformName == LIN_RASPI)
    {
        job.with {
            wrappers {
                buildInDocker {
                    dockerfile(docker_dir, docker_file)
                    verbose()
                    volume(buildDir, buildDir)
                    volume(sourceDir, sourceDir)
                }
            }
        }
    }else
        return;
}

void GetCMakeSteps(descriptor, job, variant, level, source_dir, build_dir)
{
    cmake_args = "-DCMAKE_TOOLCHAIN_FILE=${descriptor.cmake_toolchain} "
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
        steps {
            cmake {
                generator("${descriptor.cmake_generator}")
                args("-DCMAKE_INSTALL_PREFIX=out ${cmake_args}")
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
        /* Fixes some issues on Mac OS X with finding Brew */
        properties {
            environmentVariables {
                keepSystemVariables(true)
                keepBuildVariables(true)
            }
        }
    }

    if(level == 1)
    {
        /* Add artifact step, archiving the binaries */
        job.with {
            publishers {
                textFinder("The following tests FAILED",'', true, false, true)
                archiveArtifacts {
                    pattern("out/**")
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
    if(descriptor.platformName == WIN_WIN32
       || descriptor.platformName == WIN_MSUWP)
    {
        /* Linking library directories */
//        compile.with {
//            steps {
//                batchFile(
//                  """
//                    mkdir build_Debug
//                    mkdir build_Release
//                    mklink /J build_Debug\\libs libs
//                    mklink /J build_Release\\libs libs
//                    exit 0
//                  """)
//            }
//        }
    }else if(descriptor.platformName == MAC_MCOSX)
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

for(t in Targets) {
    branch = "testing"
    pipelineName = "${PROJECT_NAME}_${t.platformName}_${t.platformArch}"
    sourceDir = "${WORKSPACE}/${PROJECT_NAME}_${branch}_src"

    t.cmake_preload = "${sourceDir}/cmake/Preload/${t.cmake_preload}"
    t.cmake_toolchain = "${sourceDir}/cmake/Toolchains/${t.cmake_toolchain}"

    /* Create a pipeline per build target */
    pip = deliveryPipelineView("${pipelineName}")

    /* Acquiring the source code is step 0 */
    source_step = job("0.0_${pipelineName}_Source")
    /* One function to insert the SCM data */
    GetSourceStep(t, sourceDir, source_step, branch)

    source_step.with {
        customWorkspace(sourceDir)
    }

    pip.with {
        allowPipelineStart(true)
        showTotalBuildTime(true)
        pipelines {
            component(pipelineName, source_step.name)
        }
    }

    last_step = source_step.name
    i = 1

    for(rel in RELEASE_TYPES)
    {
        def compile = job("${i}.0_${pipelineName}_${rel}")
        def testing = job("${i}.1_${pipelineName}_${rel}_Testing")

        def workspaceDir = "${WORKSPACE}/${pipelineName}_build_${rel}"

        i++;

        /* Compilation and testing will only be performed on suitable hosts */
        compile.with {
            label(t.label)
            customWorkspace(workspaceDir)
            deliveryPipelineConfiguration(pipelineName, "${rel} compilation stage")
            triggers {
                upstream(last_step)
            }
        }
	if(t.do_tests)
	{
	    testing.with {
		label(t.label)
		customWorkspace(workspaceDir)
		deliveryPipelineConfiguration(pipelineName, "${rel} testing stage")
		triggers {
		    upstream(compile.name)
		}
	    }
	    last_step = testing.name
	}else
	{
	    last_step = compile.name
	}


        GetJobQuirks(t, compile, testing, workspaceDir)

	def buildDir = workspaceDir

        GetCMakeSteps(t, compile, rel, 0, sourceDir, buildDir)
        GetCMakeSteps(t, testing, rel, 1, sourceDir, buildDir)

        GetDockerDataLinux(t, compile, sourceDir, buildDir)
        GetDockerDataLinux(t, testing, sourceDir, buildDir)
    }
}
