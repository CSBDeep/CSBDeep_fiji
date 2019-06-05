#@ File(label="Input path (.tif or folder with .tifs)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script', style="both") input
#@ File(label="Output path (.tif or folder)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script/out', style="directory") output
#@ File(label="Model file", required=false, value='/home/random/Development/imagej/project/CSBDeep/data/Tobias Boothe/models/phago_C2_no_transform_model.zip', style="file") modelFile
#@ String(label="Model file", required=false, value='phago_C2_no_transform_model') _modelName
#@ Integer(label="Number of tiles", required=false, value=8) nTiles
#@ Integer(label="Tile overlap", required=false, value=32) overlap
#@ Boolean(label="Normalize input", required=false, value=true) normalizeInput
#@ Float(label="Bottom percentile", required=false, value=3.0, stepSize=0.1) percentileBottom
#@ Float(label="Top percentile", required=false, value=99.8, stepSize=0.1) percentileTop
#@ Boolean(label="Clip", required=false, value=false) clip
#@ Boolean(label="Show progress dialog", required=false, value=true) showProgressDialog
#@ DatasetIOService io
#@ CommandService command
#@ ModuleService module

from java.io import File
import sys
from de.csbdresden.csbdeep.commands import GenericNetwork
from ij import IJ
from ij.plugin import Duplicator
import os

def getFileName(path):
	fileparts = path.split("/")
	return fileparts[len(fileparts)-1]

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



input = str(input) # change object from file to str
output = str(output)  # change object from file to str

if input.endswith(".tif"):
	if output.endswith(".tif"):
		impSource = io.open(input)
		runNetwork(input, output, impSource)
	else:
		if not(output.endswith("/")):
			output += "/"

		impSource = IJ.openImage(input)

		[width, height, nChannels, nSlices, nFrames] = impSource.getDimensions()

		if nChannels > 1: # if more than 1 channel, exit
			print("ERROR: please provide an image with a single channel")
			sys.exit()	

		print("Processing: " + getFileName(input))
		print("Found "+str(impSource.getNFrames())+" frames to process")

		for dt in xrange(nFrames): # For each frame
			 # Frames in the movie will begin at 1
			frame = dt+1
			# Duplicate the frame of interest
			impSingleTp = Duplicator().run(impSource, 1, 1, 1, nSlices, frame, frame)
			# Create a new output fileName
			root, ext = os.path.splitext(getFileName(input))
			outputFileName = root + "_frame" + str(frame) + ".tif"
			# Run the network
			print("Processing: " + outputFileName)
			runNetwork(input, output + outputFileName, impSingleTp)

	
else:
	if output.endswith(".tif"):
		print("ERROR: please provide a directory as output, because your input is also a directory")
		sys.exit()
	if not(output.endswith("/")):
		output += "/"
	if not(input.endswith("/")):
		input += "/"
	if(output == input):
		print("ERROR: please provide an output directory that is not the same as the input directory")
		sys.exit()
	directory = File(input);
	listOfFilesInFolder = directory.listFiles();

	for file in listOfFilesInFolder:
		if file.toString().endswith(".tif"):
			impSource = io.open(file.toString())
			runNetwork(file.toString(), output + getFileName(file.toString()), impSource)
