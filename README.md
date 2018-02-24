# CSBDeep Fiji Plugin

## Install

### ImageJ update site
The CSBDeep plugin can be installed from the ImageJ update site `http://sites.imagej.net/CSBDeep/`. See the [CSBDeep Wiki Pages](https://github.com/CSBDeep/CSBDeep/wiki) for more details.

### From source
1. Clone this repository.
2. Run the following command from inside the repo:
```
mvn -Dimagej.app.directory=/path/to/Fiji.app/ -Ddelete.other.versions=true
```

## Run
1. Download the [examplary image data](http://csbdeep.bioimagecomputing.com/exemplary-image-data.zip)
2. Open Fiji.
3. Open an example image, e.g. `tribolium.tif`.
4. Run the plugin via `Plugins > CSBDeep`.
5. Run the plugin by pressing `Ok`.

If all goes well, an image will be displayed representing the result of the model execution.

See the [CSBDeep Wiki Pages](https://github.com/CSBDeep/CSBDeep/wiki) for more details.

## Develop

### Code Style

If you use eclipse you can import our code formatter `doc/eclipse-code-formatter.xml`, code cleanup (`doc/eclipse-code-clean-up.xml`) and import order (`eclipse-import-order.importorder`) settings.

## GPU support

For GPU support we load the TensorFlow JNI with GPU support manually when a command is initialized. This means that the GPU version of the TensorFLow JNI must be accessible in the java library path (For example `Fiji.app/lib/linux64` in a Fiji installation).

See the according [CSBDeep Wiki page](https://github.com/mpicbg-csbd/CSBDeep/wiki/CSBDeep-in-Fiji-–-Installation#gpu-support) for a detailed installation guide.

### Muliple GPUs

See the according [CSBDeep Wiki page](https://github.com/mpicbg-csbd/CSBDeep/wiki/CSBDeep-in-Fiji-–-Installation#multiple-gpus).

## License

This project is licensed under the BSD 2-clause "Simplified" License -- see the [LICENSE.txt](LICENSE.txt) file for details.

