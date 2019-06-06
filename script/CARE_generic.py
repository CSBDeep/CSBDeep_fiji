# @File(label="Input path (.tif or folder with .tifs)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script', style="both") input
# @File(label="Output path (.tif or folder)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script/out', style="directory") output
# @File(label="Model file", required=false, value='/home/random/Development/imagej/project/CSBDeep/data/Tobias Boothe/models/phago_C2_no_transform_model.zip', style="file") modelFile
# @String(label="Model file", required=false, value='phago_C2_no_transform_model') _modelName
# @Integer(label="Number of tiles", required=false, value=8) nTiles
# @Integer(label="Tile overlap", required=false, value=32) overlap
# @Boolean(label="Normalize input", required=false, value=true) normalizeInput
# @Float(label="Bottom percentile", required=false, value=3.0, stepSize=0.1) percentileBottom
# @Float(label="Top percentile", required=false, value=99.8, stepSize=0.1) percentileTop
# @Boolean(label="Clip", required=false, value=false) clip
# @Boolean(label="Show progress dialog", required=false, value=true) showProgressDialog
# @DatasetIOService io
# @CommandService command
# @ModuleService module

from java.io import File
import sys
from de.csbdresden.csbdeep.commands import GenericNetwork
from ij import IJ
from ij.plugin import Duplicator
import os

def runNetwork(inputPath, outputPath, imp):
	print("input: " + inputPath + ", output: " + outputPath)
	mymod = (command.run(GenericNetwork, False,
		"input", imp,
		"nTiles", nTiles,
		"overlap", overlap,
		"normalizeInput", normalizeInput,
		"percentileBottom", percentileBottom,
		"percentileTop", percentileTop,
		"clip", clip,
		"showProgressDialog", showProgressDialog,
		"modelFile", modelFile)).get()
	myoutput = mymod.getOutput("output")
	print(myoutput)
	io.save(myoutput, outputPath)


inputPath = input.getPath() # Extract input file path
outputPath = output.getPath()  # Extract output file path

if inputPath.endswith(".tif"): # If processing a single .tif file
	
	impSource = IJ.openImage(inputPath) 

	[width, height, nChannels, nSlices, nFrames] = impSource.getDimensions()
	if nChannels > 1: # if more than 1 channel, exit
		print("ERROR: please provide an image with a single channel")
		sys.exit()	

	if outputPath.endswith(".tif"):
		if nFrames > 1: # If there is only 1 specified .tif file but multiple frames
			print("ERROR: To process an hyperstack with multiple frames, please provide a directory as output")
			sys.exit()
		runNetwork(inputPath, outputPath, impSource)

	else:
		print("Processing: " + input.getName())
		print("Found "+str(impSource.getNFrames())+" frames to process")

		for dt in xrange(nFrames): # For each frame
			frame = dt+1 # Frames in the movie will begin at 1
			# Duplicate the frame of interest
			impSingleTp = Duplicator().run(impSource, 1, 1, 1, nSlices, frame, frame)
			# Create a new output fileName
			root, ext = os.path.splitext(input.getName())
			outputFileName = root + "_frame" + str(frame) + ".tif"
			# Run the network
			print("Processing: " + outputFileName)
			runNetwork(inputPath, os.path.join(outputPath, outputFileName), impSingleTp)
	
elif input.isDirectory():
	if outputPath.endswith(".tif"):
		print("ERROR: please provide a directory as output, because your input is also a directory")
		sys.exit()
	if(outputPath == inputPath):
		print("ERROR: please provide an output directory that is not the same as the input directory")
		sys.exit()

	listOfFilesInFolder = input.listFiles();
	for file in listOfFilesInFolder:
		if file.getName().endswith(".tif"):
			impSource = io.open(file.toString())
			runNetwork(file.toString(), os.path.join(outputPath, file.getName()), impSource)

else: # input is not a directory but not a .tif file either
	print("ERROR: please provide a .tif file or directory for input")
	sys.exit()
