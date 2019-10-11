[![](https://travis-ci.com/CSBDeep/CSBDeep_fiji.svg?branch=master)](https://travis-ci.com/CSBDeep/CSBDeep_fiji)

# CSBDeep Fiji Plugin

## Install

### ImageJ update site
The CSBDeep plugin can be installed from the ImageJ update site `http://sites.imagej.net/CSBDeep/`. See the [CSBDeep Wiki Pages](https://github.com/CSBDeep/CSBDeep_website/wiki/CSBDeep-in-Fiji) for more details.

### From source
1. Clone this repository.
2. Run the following command from inside the repo:
```
mvn -Dimagej.app.directory=/path/to/Fiji.app/ -Ddelete.other.versions=true
```

## Run demos
1. Download the [examplary image data](http://csbdeep.bioimagecomputing.com/exemplary-image-data.zip)
2. Open Fiji.
3. Open an example image, e.g. `tribolium.tif`.
4. Run the plugin via `Plugins > CSBDeep > Demo`.
5. Run the plugin by pressing `Ok`.

If all goes well, an image will be displayed representing the result of the model execution.

See the [CSBDeep Wiki Pages](https://github.com/CSBDeep/CSBDeep_website/wiki/CSBDeep-in-Fiji) for more details.

## Run your own model
1. Use the [python code](https://github.com/CSBDeep/CSBDeep) to train your network with your data. Export it as ZIP.
2. Open Fiji.
3. Open an image.
4. Run the plugin for any network via `Plugins > CSBDeep > Run your network`.
5. Load your exported network by pressing `Browse` on the `Import model (.zip)` line.
5. Run the plugin by pressing `Ok`.

If all goes well, an image will be displayed representing the result of the model execution.

See the [CSBDeep Wiki Page](https://github.com/CSBDeep/CSBDeep_website/wiki/Your-Model-in-Fiji) for more details.

## Develop

### Code Style

If you use eclipse you can import our code formatter `doc/eclipse-code-formatter.xml`, code cleanup (`doc/eclipse-code-clean-up.xml`) and import order (`eclipse-import-order.importorder`) settings.

## Switching the TensorFlow version

For supporting a model trained with a specific TensorFlow version or for GPU support, one has to install a specific native TensorFlow JNI. In order to achieve that, do the following:
1. Open `Edit > Options > TensorFlow...`
- Choose the version matching your system / model
- Wait until a message opens telling you that the library was installed
- Restart Fiji

## GPU support

For GPU support, two things have to be done:
- install CUDA and CuDNN and make sure Fiji knows about the installation paths
- install a TensorFlow version with GPU support (see section above)

For further details please refer to the [CSBDeep Wiki page](https://github.com/CSBDeep/CSBDeep_website/wiki/CSBDeep-in-Fiji-–-Installation#gpu-support).

### Muliple GPUs

See the according [CSBDeep Wiki page](https://github.com/CSBDeep/CSBDeep_website/wiki/CSBDeep-in-Fiji-–-Installation#multiple-gpus).

## License

This project is licensed under the BSD 2-clause "Simplified" License -- see the [LICENSE.txt](LICENSE.txt) file for details.
