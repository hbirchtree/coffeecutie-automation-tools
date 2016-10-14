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
                external("groovy/${s.seedFile}")
                removeAction("DELETE")
                removeViewAction("DELETE")
                ignoreExisting(false)
            }
        }
    }
}
