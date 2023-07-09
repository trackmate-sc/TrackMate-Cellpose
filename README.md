[![Build Status](https://github.com/trackmate-sc/TrackMate-Cellpose/actions/workflows/build.yml/badge.svg)](https://github.com/trackmate-sc/TrackMate-Cellpose/actions/workflows/build.yml)

# TrackMate-Omnipose
Tentative Omnipose integration in TrackMate starting from the already existing [Cellpose integration in TrackMate](https://github.com/trackmate-sc/TrackMate-Cellpose).
It works with only little code changes.

## Example
[Time-lapse captured by Rodrigo Arias Cartin, Frédéric Barras lab, Institut Pasteur](

https://github.com/marieanselmet/TrackMate-Omnipose/assets/32811540/3c2365c9-8d1b-4057-b4d1-2939e4e2b818

)

*Time-lapse captured by Rodrigo Arias Cartin, Frédéric Barras lab, Institut Pasteur*


## Version
This code works with the Omnipose version 0.3.6. It doesn't work with the last version of Omnipose (same issue as for the [Omnipose utility wrapper of the BIOP](https://github.com/BIOP/ijl-utilities-wrappers)).

I set my Windows installation as follows, to work on GPU:
```
conda create -n omnipose
conda activate omnipose
conda install pytorch==2.0.0 torchvision==0.15.0 torchaudio==2.0.0 pytorch-cuda=11.8 -c pytorch -c nvidia
pip install omnipose==0.3.6
pip install cellpose-omni==0.7.3
```

The default model *bact_phase_omni* is stored in cellpose pretrained models folder.
