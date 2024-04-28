
### How to build and release Ginj

1. Change sources
2. Update version number in:
    - src/main/java/info/ginj/Ginj.java
    - pom.xml
    - build_installer.install4j
   to e.g. "0.4.5" or "0.4.5-pre"
3. Commit and push (possibly after merging branch onto master)
4. Build the project: clean, then package. It will run maven's assembly plugin to create a single jar.
5. Rename the resulting file in target/ from e.g. Ginj-0.4.5-jar-with-dependencies.jar to just Ginj.jar
6. Delete previous version from releases/ folder
7. Launch Install4j, then:
    - check that the version has been updated in the "Application info" tab
    - check that "build all" is selected in the "Build" tab on the left
    - click the "build project" button at the top
8. Go to https://github.com/Ginj-capture/Ginj/releases and click "Draft a new release" on the right
    - fill the "Tag version" field with e.g "v0.4.5"
    - fill the "Release title" field with e.g. "Release v0.4.5"
    - fill the "Describe this release" with a text. Use hyphens to list the changes
    - from the target/ folder, upload the Ginj.jar file
    - from the releases/ folder, upload all files except the output.txt
    (Github will automatically add the source jar)
    - if it is a pre-release (means that upgrade must not be proposed automatically), check the "This is a pre-release" box
    - click "Publish release"
    
Done !