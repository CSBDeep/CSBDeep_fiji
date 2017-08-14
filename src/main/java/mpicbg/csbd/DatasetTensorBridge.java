package mpicbg.csbd;

import java.util.Arrays;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.tensorflow.Shape;
import org.tensorflow.Tensor;

public class DatasetTensorBridge {
	
	private Dataset dataset;
    private Shape inputTensorShape;

	private List<String> datasetDimNames;
    private List<Integer> datasetDimIndices;
    private List<Long> datasetDimLengths;
    private int[] dimMapping = {0,1,2,3,4};
    private boolean mappingInitialized = false;  
    
    public DatasetTensorBridge(Dataset image){
    	dataset = image;
    	datasetDimNames = Arrays.asList("X","Y","Z","Ch","T");
		datasetDimIndices = Arrays.asList(image.dimensionIndex(Axes.X),
				image.dimensionIndex(Axes.Y), 
				image.dimensionIndex(Axes.Z), 
				image.dimensionIndex(Axes.CHANNEL), 
				image.dimensionIndex(Axes.TIME));
		datasetDimLengths = Arrays.asList(image.getWidth(), 
				image.getHeight(), 
				image.getDepth(), 
				image.getChannels(), 
				image.getFrames());	
    }
    
    public int getDatasetDimLengthFromTFIndex(int fakeTFIndex){
		if(dimMapping[fakeTFIndex] < 0){
			return 1;
		}
		return datasetDimLengths.get(dimMapping[fakeTFIndex]).intValue();
	}
	
    public int getDatasetDimIndexByTFIndex(int fakeTFIndex){
		if(dimMapping[fakeTFIndex] < 0){
			return -1;
		}
		return datasetDimIndices.get(dimMapping[fakeTFIndex]);
	}

	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void setMappingInitialized(boolean mappingInitialized) {
		this.mappingInitialized = mappingInitialized;
	}

	public void setMappingDefaults() {
		System.out.println("setmappingdefaults");
		setMappingInitialized(true);
		if(inputTensorShape.numDimensions() == 5){
			dimMapping[0] = 4;
			dimMapping[1] = 2;
			dimMapping[2] = 1;
			dimMapping[3] = 0;
			dimMapping[4] = 3;
		}else{
			if(inputTensorShape.numDimensions() == 4){
				dimMapping[0] = -1;
				dimMapping[1] = 4;
				dimMapping[2] = 1;
				dimMapping[3] = 0;
				dimMapping[4] = 3;
			}else{
				return;
			}
		}
		
	}
	
    public void setInputTensorShape(Shape shape){
    	inputTensorShape = shape;
    }
	
    public Shape getInputTensorShape() {
		return inputTensorShape;
	}
    
    public int numDimensions(){
    	return datasetDimNames.size();
    }
    
    public long getDatasetDimLength(int index){
    	return datasetDimLengths.get(index);
    }
    
    public String getDatasetDimName(int index){
    	return datasetDimNames.get(index);
    }
    
    public int getMapping(int fakeTFIndex){
    	return dimMapping[fakeTFIndex];
    }
    
    public void setMapping(int fakeTFIndex, int mapping){
    	dimMapping[fakeTFIndex] = mapping;
    }
    
    public void setMappingRealTFIndex(int realTFIndex, int mapping){
    	dimMapping[realTFIndex+dimMapping.length-inputTensorShape.numDimensions()] = mapping;
    }
    
    public float[][][][][] createFakeTFArray(){
//    	System.out.println("create fake tf array with dims: " + getDatasetDimLengthFromTFIndex(0) + " " + getDatasetDimLengthFromTFIndex(1) + " " + getDatasetDimLengthFromTFIndex(2) + " " + getDatasetDimLengthFromTFIndex(3) + " " + getDatasetDimLengthFromTFIndex(4));
    	return new float[getDatasetDimLengthFromTFIndex(0)]
    					[getDatasetDimLengthFromTFIndex(1)]
    					[getDatasetDimLengthFromTFIndex(2)]
						[getDatasetDimLengthFromTFIndex(3)]
						[getDatasetDimLengthFromTFIndex(4)];
    }

	public boolean complete() {
		return inputTensorShape != null && dataset != null;
	}
	
	public void printMapping(){
		System.out.println("--------------");
		System.out.print("mapping:");
		for(int i : dimMapping){
			System.out.print(" " + i);
		}
		System.out.println();
		System.out.print("datasetDimIndices:");
		for(int i : datasetDimIndices){
			System.out.print(" " + i);
		}
		System.out.println();
		System.out.println("--------------");
	}
	
	public boolean moveZMappingToFront(){
		boolean shift = false;
		printMapping();
		for(int i = numDimensions()-1; i >= 0; i--){
			if(dataset.dimensionIndex(Axes.Z) == getMapping(i)){
				shift = true;
			}
			if(shift&& i > 0){
				setMapping(i, getMapping(i-1));									
			}
		}
		printMapping();
		return shift;
	}

	public float[][][][][] createFakeTFArray(Tensor output_t) {
		int[] dims = {1,1,1,1,1};
		int diff = dims.length-output_t.numDimensions();
		for(int i = 0; i < output_t.numDimensions(); i++){
			dims[diff+i] = (int) output_t.shape()[i]; 
		}
//		System.out.println("create fake tf output array with dims: " + dims[0] + " " + dims[1] + " " 
//						+ dims[2] + " " + dims[3] + " " + dims[4]);
    	return new float[dims[0]][dims[1]][dims[2]][dims[3]][dims[4]];
	}

	public Dataset createFromTFDims(long[] tfdims) {
		
//		for(int i = 0; i < tfdims.length; i++){
//			System.out.println("tfdims " + i + ": " + tfdims[i]);
//		}
		
		int tfDimLength = tfdims.length;
		
		int count = 0;
		for(int i = numDimensions()-tfDimLength; i < numDimensions(); i++){
			int dataset_i = getDatasetDimIndexByTFIndex(i);
			if(dataset_i >= 0){
				count ++;
			}
		}
		
		long[] _dims = new long[numDimensions()];
		CalibratedAxis[] _axes = new CalibratedAxis[numDimensions()];
		
		printMapping();
		
		int tfi = 0;
		for(int i = 0; i < numDimensions(); i++){
			int dataset_i = getDatasetDimIndexByTFIndex(i);
//			System.out.println("i: " + i + " dataset_i " + dataset_i);
			if(i >= numDimensions() - tfDimLength){
				if(dataset_i >= 0){
					_dims[dataset_i] = tfdims[tfi];
					_axes[dataset_i] = dataset.axis(dataset_i);
//					System.out.println("set dataset dim " + dataset_i + " from tf dim " + i);
				}
				tfi++;
			}
			
		}
		
//		System.out.println("count: " + count);
//		
//		for(int i = 0; i < _dims.length; i++){
//			System.out.println("_dataset output dim " + i + ": " + _axes[i] + " -> " + _dims[i]);
//		}
		
		long[] dims = new long[count];
		CalibratedAxis[] axes = new CalibratedAxis[count];
		
		int j = 0;
		for(int i = 0; i < _dims.length; i++){
			if(_dims[i] > 0){
				dims[j] = _dims[i];
				axes[j] = _axes[i];
				j++;
			}
		}
		
//		for(int i = 0; i < dims.length; i++){
//			System.out.println("dataset output dim " + i + ": " + axes[i] + " -> " + dims[i]);
//		}
				
//		System.out.println("dims length: " + dims.length);
//		System.out.println("dataset length: " + dataset.numDimensions());
		Dataset img_out = dataset.factory().create(dims, new UnsignedByteType());
//		System.out.println("img dims length: " + img_out.numDimensions());
		img_out.setAxes(axes);
		return img_out;
	}

}
