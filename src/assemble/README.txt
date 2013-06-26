This is the GobyWeb plugin Software Development Kit distribution.
For more information, see http://campagnelab.org/software/gobyweb/plugins-sdk/

INSTALLATION

After download, uncompress the archive as follows:

tar -zxvf plugins-VERSION-sdk.tar.gz

This creates the following files and directories:

    $ plugins-sdk-VERSION
       |
       |README.txt (This file)
       |
       |--bin
       |  |--plugins-*
       |
       |--lib
       |   |--plugins-sdk.jar
       |
       |--templates
          |--scripts
             |--*.sh

The bin folder includes the plugins commands, the lib folder includes the
software library and the templates folder includes some stubs scripts that
can be used as basis for developing new plugins.

The only required installation step is to configure the command line interface.

Add plugins-export-env to the $HOME/.bash_profile as follows, replacing
$INSTALLATION_DIRECTORY with the location where you decompressed the
distribution:

source $INSTALLATION_DIRECTORY/bin/plugins-export-env
