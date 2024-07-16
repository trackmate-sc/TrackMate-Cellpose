[![Build Status](https://github.com/trackmate-sc/TrackMate-Cellpose/actions/workflows/build.yml/badge.svg)](https://github.com/trackmate-sc/TrackMate-Cellpose/actions/workflows/build.yml)

# Omnipose integration in TrackMate.

The Omnipose integration in TrackMate works roughly as the Cellpose integration one. 
It requires Omnipose to be installed on your system and working independently. This page gives installation details and advices at how to use the omnipose integration in TrackMate.

## Omnipose
Omnipose is a segmentation algorithm based on Deep-Learning techniques, and inspired from the Cellpose architecture. Omnipose is well suited for bacterial cell segmentation, and achieves remarkable performances on mixed bacterial cultures, antibiotic-treated cells and cells of elongated or branched morphology.

If you use the Omnipose TrackMate module for your research, please also cite the Omnipose paper:
[Cutler, K.J., Stringer, C., Lo, T.W. et al. Omnipose: a high-precision morphology-independent solution for bacterial cell segmentation. Nat Methods 19, 1438–1448 (2022)](https://www.nature.com/articles/s41592-022-01639-4).



## Example
https://github.com/marieanselmet/TrackMate-Omnipose/assets/32811540/3c2365c9-8d1b-4057-b4d1-2939e4e2b818

*E. coli, Marie Anselmet and Rodrigo Arias Cartin, Barras lab, Institut Pasteur*


### Omnipose installation

The integration works with Omnipose version 1.0.6.

Like for the cellpose integration, you need to have a working Python installation of omnipose on the computer you want to use this extension with.
To install Omnipose, you can refer directly to the installation guide provided on the [Omnipose release page on PypI](https://pypi.org/project/omnipose/), but some steps may generate errors.

We give below a tested procude to install omnipose on a Mac Intel, Mac M1 to M3 and Windows with GPU support via mamba. 

An example Windows installation working on GPU (may need to adapt the cuda version to your drivers):
```sh
mamba create -n omnipose-106 'python==3.9.18' pytorch torchvision pytorch-cuda=11.8 -c pytorch -c nvidia -y
mamba activate omnipose-106
pip install natsort
pip install scipy==1.11.4
pip install omnipose==1.0.6
```

An example Mac Intel installation:
```sh
mamba create -n omnipose-106 'python==3.9.18'
mamba activate omnipose-106
mamba install pytorch torchvision -c pytorch
pip install natsort
pip install scipy==1.11.4
pip install fastremap==1.12.2
pip install omnipose==1.0.6
```

An example Mac M1 to M3 installation:
```sh
mamba create -n omnipose-106 'python==3.9.18' 
mamba  activate omnipose-106 
mamba install pytorch torchvision -c pytorch
pip install natsort
pip install scipy==1.11.4
pip install omnipose==1.0.6
```

### Troubleshooting "Found 0 spots" errors with pretrained models

On some systems we have noticed that sometimes TrackMate returns 0 detections for the cellpose and omnipose detectors, even when the installation of these two programs worked correctly.
In most cases, this is due to the fact that the pretrained models have not been downloaded prior to running the TrackMate integration. If the model your trying to call doesn't exist on your computer, a message should appear in the TrackMate logger after the preview step.
To fix this, the easiest way is to launch the omnipose Python GUI, and segment a single small image.
This will trigger the download of the pretrained models.
After this, the TrackMate Omnipose integration should work as expected.

### Custom models

You can use in this TrackMate-Omnipose integration your own custom models that were trained on the same version 1.0.6 of Omnipose. 
In the last versions of Omnipose, some changes were made, in particular in the way to call the models and the default parameters values. We made our code as robust as possible to deal with these changes but we can't ensure this TrackMate integration of Omnipose is compatible with custom models trained on all the previous versions of Omnipose.

### Omnipose advanced detector

Choosing the Omnipose advanced detector gives the possibility to tune the values of the mask and the flow threshold.
Details about the meaning of the mask and flow thresholds can be found [here](https://omnipose.readthedocs.io/settings.html).
