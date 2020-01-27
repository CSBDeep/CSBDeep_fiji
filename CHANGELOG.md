# Changelog

## 0.5.0
- updated to `imagej-tensorflow 1.1.4`
  - native JNIs can now be switched via `Edit > Options > TensorFlow...`

## 0.4.1
-  `GenericCoreNetwork`: remove menu path

## 0.4.0
- Adds `GenericTableNetwork`
  - add `scijava-plugins-io-table` dependency
  - rename `DefaultOutputProcessor` to `DatasetOutputProcessor`
  - add `TableOutputProcessor`
  - move everything but the `output` Parameter and the initialization of the `OutputProcessor` from `GenericNetwork` to `GenericCoreNetwork`
  - `GenericCoreNetwork`: `protected abstract initOutputProcessor` method
  - `GenericCoreNetwork`: `protected abstract computeOutput` method
  - make `GenericNetwork` extending `GenericCoreNetwork` with `Dataset` output parameter
  - create `GenericTableNetwork` extending `GenericCoreNetwork` with `GenericTable` output parameter
  - `OutputProcessor` is typed now, changed signature of `run`
  - `ImageTensor`: add `tilingAllowed` attribute
  - `ImageTensor`: make a few methods private
- `POM`: `pom-scijava 23.2.0` &rarr; `27.0.1`
-  `PercentileNormalizer`: Fix normalization for images with the same value for all pixels
  - Images with the same value were normalized to images of value `NaN`
  - now they return an image of value `0.0`

## 0.3.4
- don't load `meta.json` of model since we do not use it yet

## 0.3.2
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
