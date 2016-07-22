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
