# CSBDeep Fiji Plugin

## Install
1. Clone this repository.
2. Run the following command from inside the repo:
```
mvn -Dimagej.app.directory=/path/to/Fiji.app/ -Ddelete.other.versions=true
```

## Run
1. Clone data repository https://github.com/frauzufall/CSBDeep-data.
2. Open Fiji.
3. Open an image from the data repo, e.g. `CSBDeep-data/net_denoise/input.png`.
4. Run the plugin via `Plugins > CSBDeep`.
5. Chose an `Import model`, e.g. `CSBDeep-data/net_denoise/resunet.pb`. In this case, you still have to adjust the input node name to `input_1`, for the other models `input` should be fine.
6. Run the plugin by pressing `Ok`.

The first image popping up is the normalized image (for debugging).

If all goes well, another image will be displayed representing the result of the model execution.

## Develop

### Code Style

If you use eclipse you can import our code formatter `doc/eclipse-code-formatter.xml`, code cleanup (`doc/eclipse-code-clean-up.xml`) and import order (`eclipse-import-order.importorder`) settings.

## GPU support

For GPU support we load the TensorFlow JNI with GPU support manually when a command is initialized. This means that the GPU version of the TensorFLow JNI must be accessible in the java library path (For example `Fiji.app/lib/linux64` in a Fiji installation).

See the according [CSBDeep Wiki page](https://github.com/mpicbg-csbd/CSBDeep/wiki/CSBDeep-in-Fiji-–-Installation#gpu-support) for a detailed installation guide.

### Muliple GPUs

See the according [CSBDeep Wiki page](https://github.com/mpicbg-csbd/CSBDeep/wiki/CSBDeep-in-Fiji-–-Installation#multiple-gpus).
