# @String(label="Input path (.tif or folder with .tifs)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script') input
# @String(label="Output path (.tif or folder)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script/out') output
# @String(label="Model file", required=false, value='/home/random/Development/imagej/project/CSBDeep/data/Tobias Boothe/models/phago_C2_no_transform_model.zip') modelFile
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

// modified from python https://github.com/CSBDeep/CSBDeep_fiji/blob/master/script/CARE_generic.py
// to groovy 
import de.csbdresden.csbdeep.commands.GenericNetwork

def runNetwork(inputPath, outputPath){
	print("\ninput: " + inputPath + ", output: " + outputPath)
	def imp = io.open(inputPath)
	def mymod = (command.run(GenericNetwork, false,
		"input", imp,
		"nTiles", nTiles,
		"overlap", overlap,
		"normalizeInput", normalizeInput,
		"percentileBottom", percentileBottom,
		"percentileTop", percentileTop,
		"clip", clip,
		"showProgressDialog", showProgressDialog,
		"modelFile", modelFile)).get()
	def myoutput = mymod.getOutput("output")
	print("\noutput: " +myoutput)
	io.save(myoutput, outputPath)
}
// Check that input and ouput are different (to avoid overwriting files)
if((output == input)) return print("ERROR: please provide an output file/directory that is not the same as the input file/directory")

// convert in/output strings to File
def input_File = new File(input)
def output_File = new File(output)

// input & output are files	
if ( (input_File.isFile()) && (output_File.isFile()) ) 	runNetwork(input_File.getAbsolutePath(), output_File.getAbsolutePath())
// or folders ? 	
else if ( (input_File.isDirectory()) && (output_File.isDirectory()) ) { 
	input_File.listFiles().each{ file ->
		if (file.name.endsWith(".tif"))	runNetwork(file.getAbsolutePath(), new File(output_File, file.name).getAbsolutePath() )	
	}	
}