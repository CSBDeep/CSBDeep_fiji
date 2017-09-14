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

## GPU support
TODO -> https://github.com/frauzufall/CSBDeep/issues/1

For GPU support put the native library with GPU support for tensorflow ([https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.2.0.tar.gz](https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.2.0.tar.gz)) into `Fiji.app/lib/linux64/`.

Make sure cuda and cuDNN 5.1 are in the library path (set LD_LIBRARY_PATH if necessary).

### Muliple GPUs

If you have multiple GPUs with CUDA support installed it can happen that tensorflow tries to use the wrong one. To solve this problem set the environmet variable `CUDA_VISIBLE_DEVICES` to the id of the GPU (or GPUs) you want to use. You can find out the id with the command `nvidia-smi`.

Example:

    $ nvidia-smi
    Thu Sep 14 17:39:33 2017       
    +-----------------------------------------------------------------------------+
    | NVIDIA-SMI 375.66                 Driver Version: 375.66                    |
    |-------------------------------+----------------------+----------------------+
    | GPU  Name        Persistence-M| Bus-Id        Disp.A | Volatile Uncorr. ECC |
    | Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute M. |
    |===============================+======================+======================|
    |   0  TITAN Xp            Off  | 0000:03:00.0     Off |                  N/A |
    | 23%   28C    P8     9W / 250W |      1MiB / 12189MiB |      0%      Default |
    +-------------------------------+----------------------+----------------------+
    |   1  NVS 310             Off  | 0000:A1:00.0     N/A |                  N/A |
    | 30%   56C    P0    N/A /  N/A |    294MiB /   961MiB |     N/A      Default |
    +-------------------------------+----------------------+----------------------+

    $ export CUDA_VISIBLE_DEVICES=0 && ./ImageJ-linux64
