# ij-indexing-tools

This repository contains tools for creating IntelliJ shared indexes,
which can be used to speed up importing a project in IJ.

Currently, this project only supports MacOS out of the box, due to do fixed configuration.
You also have to have an IntelliJ IDEA Ultimate installed on your computer.

## Benchmarking

Benchmarks have been run for different kinds of shared indexes available in IJ.
Results presented were obtained using IntelliJ version `2023.3.8` on a MacOS machine with an M1 processor.
Indexes were first generated using IntelliJ built-in tooling,
then hosted on a local S3 instance that serves the shared indexes when requested by IntelliJ during project import.

Sharing indexes between machines has not been benchmarked but has been successfully tested.

To obtain proper results, Intellij Registry configuration has been modified:
- `shared.indexes.wait.default` was set to TRUE, indexing starts only after the shared indexes have been downloaded

Speed comparison is done between importing a project with no available shared indexes
and importing the same project with available shared indexes.
Both cases are run with IntelliJ index caches wiped clean.

Benchmarks are run on prepared sample projects.
Two of them are available in the `example/` directory.
Another one can be generated with `com.virtuslab.example.generator.FullProjectGenerator`.

## Benchmark results

The results of the benchmarks are presented below.
All durations are wall time measurements, same as seen by the IDE user.

### Shared indexes for a whole project
Results in the table below have been obtained from benchmarking shared indexes
that are created on the basis of the whole IntelliJ project.
Indexes created in this way take into consideration both the files present in the project and dependencies
added by the build tool.

The project used for testing was a sample created with `com.virtuslab.example.generator.FullProjectGenerator`,
containing 40k source files with code references to each other and 38 dependencies.

|                           | No shared indexes | With shared indexes | Speed-up |
|---------------------------|-------------------|---------------------|----------|
| **Total duration [s]**    | 105               | 73                  | 30%      |
| **Indexing duration [s]** | 96                | 54                  | 44%      |

Shared indexes built for a project can almost cut in half the time required for indexing.
They give a significant speed-up of ~30% to the whole importing process,
which apart from indexing consists also of scanning the whole build.


### Shared indexes for JDKs
Results in the table below have been obtained from benchmarking shared indexes
that are created on the basis of JDKs used in a project, no sources or dependencies are included in the index.
Additionally, there's data from importing the same project, but using indexes built for the whole project.


The project used for testing was a sample from `examples/multi-jdk` - a Gradle project with eight modules,
each requiring a different version of the JDK. 

|                           | No shared indexes | With shared indexes | Speed-up |
|---------------------------|-------------------|---------------------|----------|
| **Total duration [s]**    | 74                | 54                  | 27%      |
| **Indexing duration [s]** | 55                | 33                  | 40%      |

It can be seen that shared indexes built for JDKs can also significantly improve the performance of project importing,
especially when the project uses multiple JDKs.

It is interesting to compare the performance with shared indexes
created for JDKs to the performance with shared indexes for the whole project.
This comparison can be seen in the table below,
note that both speed-ups are calculated with regard to the 'No shared index' case.

|                           | No shared indexes | With shared 'JDK' indexes | Speed-up | With shared 'project' indexes | 'project' index speed-up |
|---------------------------|-------------------|---------------------------|----------|-------------------------------|--------------------------|
| **Total duration [s]**    | 74                | 54                        | 27%      | 48                            | 35%                      |
| **Indexing duration [s]** | 55                | 33                        | 40%      | 28                            | 49%                      |


### Shared indexes for dependency jars
Results in the table below have been obtained from benchmarking shared indexes
that are created on the basis of dependencies used in a project, no sources.

The project used for testing was a sample from `examples/multi-jar` - a Gradle project with 39 dependencies and minimal sources.

|                           | No shared indexes | With shared indexes | Speed-up |
|---------------------------|-------------------|---------------------|----------|
| **Total duration [s]**    | 46                | 45                  | 2%       |
| **Indexing duration [s]** | 27                | 25                  | 7%       |

The improvement with shared indexes built for dependency jars is marginal,
it is significantly worse that in the cases of other two shared index types.

## Using the project

### Creating shared indexes

To use this project,
run the `com.virtuslab.shared_indexes.Main` class with commands `jdk`, `jar` or `project` and corresponding options.
This will result in a creation of shared index files in the `workspace/` directory.

### Using shared indexes in IntelliJ

For the IDE to be able to use the shared indexes, they have to be available in an S3 compatible manner,
so a server must be set-up.
This can be done locally, e.g. with MinIO.

To load the shared indexes built for project create a `intellij.yaml` file in the root of the project.
Paste this into the file:
```
sharedIndex:
  project:
    - url: http://server-url
    # e.g. http://127.0.0.1:9000/shared-index/project/full-project
    # NOTE that this address must point to the directory where index.json.xz is placed
  consents:
    - kind: project
      decision: allowed
```

For shared indexes built for JDKs,
enable downloading JDK shared indexes in the IntelliJ Registry under key `shared.indexes.jdk.download`
and paste the server URL under key `shared.indexes.jdk.download.url`.

Indexes built for dependency jars cannot be server from S3 at the moment,
they have to be copied into the IntelliJ cache folder manually.
