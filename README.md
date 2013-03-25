#### The Plugins Project

Developers and administrators of a GobyWeb instance can define new plugins to extends GobyWeb with new functionality. The Plugins project allows to:
 
* 	load and validate plugin configurations, 
* 	create and register fileset instances,
* 	create and submit (both to a local or remote cluster) task instances,
*       test the installation of resources with artifacts.

There are five categories of GobyWeb plugins:

*     **Resources**. These plugins define resources such as executable files, scripts, or data files, that other plugins can share access to.
*     **Aligners**. These plugins provide functionality to align reads to a reference genome. They are used to expose various alignment methods to the end user.
*     **Alignment Analyses**. These plugins integrate analyses methods that work on sets of alignments. These plugins take alignment files as input, where each alignment is associated with a group, and produce output files that end-users can view.
*     **Filesets**. …
*     **Tasks**. …

#### Usage

The Plugins software can be either used programmatically (as GobyWeb does) throught its API or by mean of shell scripts provided in the /bin directory. 

#### Authors and Contributors
The Plugins project is currently being developed by the members of the [Campagne laboratory](http://campagnelab.org).
