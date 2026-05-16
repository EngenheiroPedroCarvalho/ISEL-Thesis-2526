# Use case - _search blobs_

This use case was designed exclusively for the purpose of performance testing. The cloud-agnostic function definition, written in Java, can be found in `MyFunctionClass.java`, while the cloud-non-agnostic function definitions are placed inside the `./cloud-non-agnostic` directory.

The cloud-agnostic function gets triggered via HTTP requests, in which the user must specify the blob name that he wants to search for, together with the bucket name. The _listBlobs_ operation, found in the cloud-agnostic libraries, is then executed to retrieve a list of blobs contained in a given bucket, which requires a remote call to the storage service. Finally, a simple search is made to check whether any of the blob names include the requested _string_. The search results are then sent as a response to the execution request.

<p align="center">
  <img src="https://user-images.githubusercontent.com/47757441/196556354-880997f9-d0fb-4902-b627-ed283ea25f6e.jpg" width="700">
</p>
