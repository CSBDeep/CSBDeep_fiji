# Changelog

### Version 0.3.2
- !!! `groupId` and `artifactId` changed !!!
```xml
<groupId>de.csbdresden</groupId>
<artifactId>csbdeep</artifactId>
```
- package names changed in the same fashion, e.g. usage in Python script:
```python
from de.csbdresden.csbdeep.commands import GenericNetwork
```
- Commands now return `Dataset` instead of `List<Dataset>` as `output`:
```Java
@Parameter(type = ItemIO.OUTPUT)
protected Dataset output;
```
- updated to TensorFlow CPU Version 1.12.0
- Adds parameter to hide process dialog (`showProcessDialog`)
- fixes for input with singleton dimensions
- removes singleton dimensions of output
- removes unused `batchAxis` parameter
- hopefully fixing thread issues with swing actions
- prints duration time of plugin run
- tiling improved
- see github commits for details
- manageable _OOM_ throws now _WARNING_ instead of _ERROR_