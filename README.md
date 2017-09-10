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
5. Chose an `Import model`, e.g. `CSBDeep-data/net_denoise/resunet.pb`.
6. Run the plugin by pressing `Ok`.

The first image popping up is the normalized image (for debugging).

If all goes well, another image will be displayed representing the result of the model execution.

## GPU support
TODO -> https://github.com/frauzufall/CSBDeep/issues/1
