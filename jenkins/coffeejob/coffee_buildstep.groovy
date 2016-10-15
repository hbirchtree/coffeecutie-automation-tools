def RELEASE_TYPES = ['Debug', 'Release']
def LINUX_PACKAGING_OPTS = "-DCOFFEE_GENERATE_SNAPPY=ON"
def Targets = [
    /* Creates Ubuntu binaries linked against the system
     * Also outputs AppImage, Snappy and Flatpak packages
     * for better cross-compatibility.
     */
    new BuildTarget("Linux", "x86-64", "ubuntu && amd64",
                   "x86_64-linux-generic.cmake",
                   "native-linux-generic.toolchain.cmake",
                   "Ninja", LINUX_PACKAGING_OPTS),
    /* Creates Win32 applications, bog standard,
     * self-contained resources.
     */
    new BuildTarget("Windows", "x86-64", "windows && x64",
                   "x86_64-windows-generic.cmake",
                   "x86_64-windows-win32.toolchain.cmake",
                   "Visual Studio 14 2015", ""),
    /* Windows UWP produces AppX directories for containment */
    new BuildTarget("Windows UWP", "x86-64", "windows && x64",
                   "x86_64-windows-uwp.cmake",
                   "x86_64-windows-uwp.toolchain.cmake",
                   "Visual Studio 14 2015", ""),
    /* Good old OS X .app directories with some spice,
     * self-contained resources.
     */
    new BuildTarget("Mac OS X", "x86-64", "macintosh && x64",
                   "x86_64-osx-generic.cmake",
                   "native-macintosh-generic.toolchain.cmake",
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
    }

    String platformName;
    String platformArch;
    String label;

    String cmake_preload;
    String cmake_toolchain;
    String cmake_generator;
    String cmake_options;
};

/* Setting up Git SCM
 * One function to change them all
 */
void GetSourceStep(descriptor, sourceDir, job)
{
    def REPO_BRANCH = 'testing'
    def REPO_URL = 'https://github.com/hbirchtree/coffeecutie.git'

    job.with {
        label(descriptor.label)
        scm {
            git {
                remote {
                    name('origin')
                    url(REPO_URL)
                }
                branch(REPO_BRANCH)
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

void GetCMakeSteps(descriptor, job, variant, level, source_dir)
{
    cmake_args = "-DCMAKE_TOOLCHAIN_FILE=${descriptor.cmake_toolchain} "
    cmake_args += descriptor.cmake_options
    
    cmake_target = "install"
    
    if(level == 1)
    {
        if(descriptor.platformName == "Windows"
           || descriptor.platformName == "Windows UWP")
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
                buildDir("build_${variant}")
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
                    pattern("build_${variant}/out/**")
                }
            }
        }
    }
}

/* Adds platform-specific features to jobs
 * Useful for fixing issues
 */
void GetJobQuirks(descriptor, compile, testing, workspace)
{
    if(descriptor.platformName == "Windows"
       || descriptor.platformName == "Windows UWP")
    {
        /* Linking library directories */
        compile.with {
            steps {
                batchFile(
                  """
                    mkdir build_Debug
                    mkdir build_Release
                    mklink /J build_Debug\\libs libs
                    mklink /J build_Release\\libs libs
                    exit 0
                  """)
            }
        }
    }else if(descriptor.platformName == "Mac OS X")
    {
        compile.with {
            steps {
                shell(
                  """
                    cd "${workspace}/src/desktop/osx/"
                    bash "gen_icons.sh"
                  """)
            }
        }
    }
}

for(t in Targets) {
    pipelineName = "${t.platformName}_${t.platformArch}"
    workspaceDir = "/tmp/${pipelineName}"
    sourceDir = "${workspaceDir}/src"

    t.cmake_preload = "${sourceDir}/cmake/Preload/${t.cmake_preload}"
    t.cmake_toolchain = "${sourceDir}/cmake/Toolchains/${t.cmake_toolchain}"

    /* Create a pipeline per build target */
    pip = deliveryPipelineView("${pipelineName}")

    /* Acquiring the source code is step 0 */
    source_step = job("0.0_${pipelineName}_Setup")
    /* One function to insert the SCM data */
    GetSourceStep(t, sourceDir, source_step)

    source_step.with {
        customWorkspace(workspaceDir)
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
        compile = job("${i}.0_${pipelineName}_${rel}")
        testing = job("${i}.1_${pipelineName}_${rel}_Testing")

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
        testing.with {
            label(t.label)
            customWorkspace(workspaceDir)
            deliveryPipelineConfiguration(pipelineName, "${rel} testing stage")
            triggers {
                upstream(compile.name)
            }
        }

        last_step = testing.name

        GetJobQuirks(t, compile, testing, workspaceDir)
        GetCMakeSteps(t, compile, rel, 0, sourceDir)
        GetCMakeSteps(t, testing, rel, 1, sourceDir)
    }
}
