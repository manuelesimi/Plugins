Developers and administrators of a GobyWeb instance can define new plugins to extends GobyWeb with new functionality. This pages describes the plugin system and provides a reference for people who write new plugins or need to maintain existing ones.

There are five categories of GobyWeb plugins:

*     **Resources**. These plugins define resources such as executable files, scripts, or data files, that other plugins can share access to.
*     **Aligners**. These plugins provide functionality to align reads to a reference genome. They are used to expose various alignment methods to the end user.
*     **Alignment Analyses**. These plugins integrate analyses methods that work on sets of alignments. These plugins take alignment files as input, where each alignment is associated with a group, and produce output files that end-users can view.
*     **Filesets**. …
*     **Tasks**. …