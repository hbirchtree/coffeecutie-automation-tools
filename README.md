# Coffeecutie Automation Tools
This repository was created to contain Jenkins, Docker and other useful scripts that have to do with automation.
If you plan on using this with Jenkins, disable submodule processing to save disk space and time.

# What you find here
 - builders/
   - Readily made Dockerfile configurations that will compile dependencies or the Coffeecutie project
 - environments/
   - Full environments that can be used for development
 - jenkins/
   - Contains a Docker Compose configuration for Jenkins
 - jenkins/coffeejob/
   - A Jenkins seedjob that will create jobs for Android, Linux, OSX and Windows
 - jenkins/jenkins/
   - A Docker-ready Jenkins configuration (can be used stand-alone)

# How to add this to Jenkins

First, you should create a Docker container using the provided Jenkins Dockerfile. This will install the necessary plugins and etc. necessary to run all the jobs.

You will also need to have enough nodes to run all the builds and label them correctly.

Add this seed-seed job:

    class SeedJob
    {
        SeedJob(String name, String repo, String branch,
                String subdir, String seedFile)
        {
            this.name = name;
            gRepository = repo;
            gBranch = branch;
            subdirectory = subdir;
            this.seedFile = seedFile;
        }

        String name;

        String gRepository;
        String gBranch;

        String subdirectory;
        String seedFile;
    }

    def SeedJobs = [
            new SeedJob("Coffee",
                   "https://github.com/hbirchtree/coffeecutie-automation-tools.git",
                   "master", "jenkins/coffeejob/",
                   "jenkins/coffeejob/coffee_buildstep.groovy")
    ]

    for(s in SeedJobs)
    {
        seed = job(s.name)

        seed.with {
            scm {
                git {
                    remote {
                        name("origin")
                        url(s.gRepository)
                    }
                    branch(s.gBranch)
                    extensions {
                        cloneOptions {
                            shallow(true)
                        }
                        relativeTargetDirectory("scripts")
                    }
                }
            }
            steps {
                dsl {
                    external("scripts/${s.seedFile}")
                    removeAction("DELETE")
                    removeViewAction("DELETE")
                    ignoreExisting(false)
                }
            }
        }
    }
