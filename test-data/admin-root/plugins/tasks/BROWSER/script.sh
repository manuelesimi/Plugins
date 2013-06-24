#!/bin/sh

function plugin_task {

  echo  "invoke the fileset.jar to scan ${FOLDER}/${OWNER}"
  #fileset jar needs a new interface or can we reuse the job interface?
  #I would create a new Admin.jsap interface for this operation, JobInterface must remain a
  #sub-view of the area based on I/O slots.
  #The result is a txt file to display to the user. The file does not need to be stored in the fileset
  #area. It is temporary and not reusable. Better to just scp it at the end of the task execution (which is
  #synchronous.

}
