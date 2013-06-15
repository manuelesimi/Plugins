#### The Plugins Project

Developers and administrators of a GobyWeb instance can define new plugins to extend GobyWeb with new functionality. The Plugins project allows to:
 
* 	load and validate plugin configurations, 
* 	create and register fileset instances,
* 	create and submit (both to a local or remote cluster) task instances,
*   test the installation of resources with artifacts.

There are five categories of GobyWeb plugins:

*     **Resources**. These plugins define resources such as executable programs, scripts, or data files, that other plugins require and can use. Resources take advantage of artifacts, that automate the installation of a resource on a compute node.
*     **Aligners**. These plugins provide functionality to align reads to a reference genome. They are used to expose various alignment methods to the end user.
*     **Alignment Analyses**. These plugins integrate analyses methods that work on sets of alignments. These plugins take alignment files as input, where each alignment is associated with a group, and produce output files in TSV or VCF format.
*     **FileSets**. A FilesSet is a set of files logically grouped (for instance, a BAM file and its associated index can be modeled as a FileSet with two entries, whose extensions are .bam and .bam.bai). Registering FileSets assigns a unique identifier to each FileSet and provides a convenient way to refer to these files from other plugins, irrespective of their physical location (on local machine or on a remote one). Plugins may also produce and store FileSets as outcome of their computation.
*     **Tasks**. These plugins accept input FileSets and produce output FileSets. They are more general than aligners and alignment analysis plugins because they can be configured to accept arbitrary FileSets as input or output.

#### Usage

The Plugins software can be either used programmatically (as GobyWeb does) through its API or with shell scripts provided in the /bin directory.

For reference documentation, examples, best practices and usage, please visit the [Plugins SDK](http://campagnelab.org/software/gobyweb/plugins-sdk/) section of the Laboratory's website.

#### Authors and Contributors
The Plugins project is currently being developed by the members of the [Campagne laboratory](http://campagnelab.org).
