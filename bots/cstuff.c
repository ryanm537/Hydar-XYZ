#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

////intIndex is a counter which will increase every time a new array is declared
//int intIndex = 0;
//
////intArray -- an array of int arrays representing all the 1d int arrays that are or will be called.
//int numArrays = 400;
//int** intArray;
//
////capadities array -- a lis of the maximum length capacity for each array
//int capacities[400];
//
////lengths array -- a list of all the lengths of each array. This is not the maximum length of each array, but rather the length of the elements that have been assigned
//int* lengths;
//
////2d lengths array -- a list of the row length of any 2d arrays
////lengths2d[i] will be 0 if i is not 2d
//int* lengths2d;
//
////pointer to the array within the 2d array 
//int array(int length){
//	intIndex = intIndex + 1;
//	
//	if(intArray == NULL){
//		intArray = malloc(sizeof(int*) * numArrays);
//	}
//	if(lengths == NULL){
//		lengths = malloc(sizeof(int) * numArrays);
//	}
//	if(lengths2d == NULL){
//		lengths2d = malloc(sizeof(int) * numArrays);
//	}
//	
//	//create a new 2d array if more 1d arrays are required
//	if(intIndex >= numArrays){
//		do{
//			numArrays = numArrays * 2;
//		}while(intIndex > numArrays);
//		
//		int** oldIntArray = intArray;
//		
//		int** newIntArray = malloc(sizeof(int*) * numArrays);
//		memcpy(newIntArray, intArray, sizeof(int*) * (numArrays/2));
//		intArray = newIntArray;
//		
//		
//		free(oldIntArray);
//		
//		//update the lengths array as well
//		int*  oldLengths = lengths;
//		int*  old2dLengths = lengths2d;
//		int* newLengths = malloc(sizeof(int) * numArrays);
//		int* new2dLengths = malloc(sizeof(int) * numArrays);
//		memcpy(newLengths, lengths, sizeof(int) * (numArrays/2));
//		memcpy(new2dLengths, lengths2d, sizeof(int) * (numArrays/2));
//		lengths = newLengths;
//		lengths2d = new2dLengths;
//		free(oldLengths);
//		free(old2dLengths);
//	}
//	
//	intArray[intIndex-1] = malloc(sizeof(int) * length);
//	
//	//initialize the value for this array in lengths to indicatee that the array is empty
//	lengths[intIndex-1] = 0;
//	capacities[intIndex-1] = length;
//	lengths2d[intIndex-1] = 0;
//	
//	//return the index of the newly created 1d array in the 2d array
//	return intIndex - 1;
//}
//
////insert value v at index i for array cindex
//void insert(int i,  int v, int cIndex){
//	intArray[cIndex][i] = v;
//}
//
////get value i from array cindex
//int get(int i, int cIndex){
//	return intArray[cIndex][i];
//}
//
////append value v to array cindex
//void append(int v, int cIndex){
//	//check if more space is required in the array
//	if(lengths[cIndex] >= capacities[cIndex]){
//		capacities[cIndex] = capacities[cIndex] * 2;
//		int* oldArray = intArray[cIndex];
//		int* newArray = malloc(sizeof(int) * capacities[cIndex]);
//		memcpy(newArray, intArray[cIndex], sizeof(intArray[cIndex]));
//		intArray[cIndex] = newArray;
//		free(oldArray);
//	}
//	//add the new value into the array
//	intArray[cIndex][lengths[cIndex]] = v;
//	
//	//increase the corresponding value in the lengths array by one because a value is being added to that array
//	lengths[cIndex] = lengths[cIndex] + 1;
//	
//
//	
//}
//
////append elements from list l (which is length len) to array cindex
//void appendList(int l[], int cIndex, int len){
//	//check if more space is required in the array
//	while((lengths[cIndex] + len) >= capacities[cIndex]){
//		capacities[cIndex] = capacities[cIndex] * 2;
//		int* oldArray = intArray[cIndex];
//		int* newArray = malloc(sizeof(int) * capacities[cIndex]);
//		memcpy(newArray, intArray[cIndex], sizeof(intArray[cIndex]));
//		intArray[cIndex] = newArray;
//		free(oldArray);
//	}
//	//add the new values into the array
//	//memcpy(intArray[cIndex]+(sizeof(int)*(lengths[cIndex]+1)), l, sizeof(int) * len);
//	int i;
//	for(i = 0; i < len; i++){
//		intArray[cIndex][lengths[cIndex]+i] = l[i];
//	}
//	
//	//increase the corresponding value in the lengths array by one because a value is being added to that array
//	lengths[cIndex] = lengths[cIndex] + len;
//	lengths2d[cIndex] = len;
//
//	
//}
//
////multiply the values of an array by a scalar
//void smult(int s, int cIndex){
//	int i;
//	for(i = 0; i < lengths[cIndex]; i++){
//		intArray[cIndex][i] = intArray[cIndex][i] * s;
//	}
//}
//
//void createFromExisting(int values[], int l, int cIndex, int length2d){
//	lengths[cIndex] = l;
//	memcpy(intArray[cIndex], values, l*sizeof(int));
//	lengths2d[cIndex] = length2d;
//}
//
//int getLength2d(int cIndex){
//	return lengths2d[cIndex];
//}
//
//int getLength(int cIndex){
//	return lengths[cIndex];
//}
//
//// FLOATS
//// FLOATS
//// FLOATS
//// FLOATS
//// FLOATS
//
////floatIndex is a counter which will increase every time a new array is declared
//int floatIndex = 0;
//
////floatArray -- an array of float arrays representing all the 1d float arrays that are or will be called.
//int fnumArrays = 64;
//float** floatArray;
//
////capadities array -- a lis of the maximum length capacity for each array
//int fcapacities[64];
//
////flengths array -- a list of all the flengths of each array. This is not the maximum length of each array, but rather the length of the elements that have been assigned
//int flengths[64];
//
////2d flengths array -- a list of the row length of any 2d arrays
////flengths2d[i] will be 0 if i is not 2d
//int flengths2d[64];
//
////pofloater to the array within the 2d array 
//float farray(float length){
//	floatIndex = floatIndex + 1;
//	
//	if(floatArray == NULL){
//		floatArray = malloc(sizeof(float*) * fnumArrays);
//	}
//	
//	floatArray[floatIndex-1] = malloc(sizeof(float) * length);
//	//create a new 2d array if more 1d arrays are required
//	if(floatIndex >= fnumArrays){
//		do{
//			fnumArrays = fnumArrays * 2;
//		}while(floatIndex > fnumArrays);
//		
//		float** oldFloatArray = floatArray;
//		
//		float** newFloatArray = malloc(sizeof(float*) * fnumArrays);
//		memcpy(newFloatArray, floatArray, sizeof(floatArray));
//		floatArray = newFloatArray;
//		
//		free(oldFloatArray);
//		
//		//update the flengths array as well
//		int newLengths[fnumArrays];
//		int new2dLengths[fnumArrays];
//		int i;
//		for(i = 0; i < fnumArrays/2; i++){
//			newLengths[i] = flengths[i];
//			new2dLengths[i] = flengths2d[i];
//		}
//		memcpy(flengths, newLengths, sizeof(flengths));
//		memcpy(flengths2d, new2dLengths, sizeof(flengths2d));
//	}
//	
//	
//	//initialize the value for this array in flengths to indicatee that the array is empty
//	flengths[floatIndex-1] = 0;
//	fcapacities[floatIndex-1] = length;
//	flengths2d[floatIndex-1] = 0;
//	
//	//return the index of the newly created 1d array in the 2d array
//	return floatIndex - 1;
//}
//
////insert value v at index i for array cindex
//void finsert(int i,  float v, int cIndex){
//	floatArray[cIndex][i] = v;
//}
//
////get value i from array cindex
//float fget(int i, int cIndex){
//	return floatArray[cIndex][i];
//}
//
////append value v to array cindex
//void fappend(float v, int cIndex){
//	//check if more space is required in the array
//	if(flengths[cIndex] >= fcapacities[cIndex]){
//		fcapacities[cIndex] = fcapacities[cIndex] * 2;
//		float* oldArray = floatArray[cIndex];
//		float* newArray = malloc(sizeof(float) * fcapacities[cIndex]);
//		memcpy(newArray, floatArray[cIndex], sizeof(floatArray[cIndex]));
//		floatArray[cIndex] = newArray;
//		free(oldArray);
//	}
//	//add the new value floato the array
//	floatArray[cIndex][flengths[cIndex]] = v;
//	
//	//increase the corresponding value in the flengths array by one because a value is being added to that array
//	flengths[cIndex] = flengths[cIndex] + 1;
//	
//
//	
//}
//
////multiply the values of an array by a scalar
//void fsmult(float s, int cIndex){
//	int i;
//	for(i = 0; i < flengths[cIndex]; i++){
//		floatArray[cIndex][i] = floatArray[cIndex][i] * s;
//	}
//}
//
//void fcreateFromExisting(float values[], int l, int cIndex, int length2d){
//	flengths[cIndex] = l;
//	memcpy(floatArray[cIndex], values, l*sizeof(float));
//	flengths2d[cIndex] = length2d;
//}
//
//int fgetLength2d(int cIndex){
//	return flengths2d[cIndex];
//}
//
//int fgetLength(int cIndex){
//	return flengths[cIndex];
//}
//
//
// PROPRIETARY STUFF FOR PYTHON FILES
// PROPRIETARY STUFF FOR PYTHON FILES
// PROPRIETARY STUFF FOR PYTHON FILES
// PROPRIETARY STUFF FOR PYTHON FILES
// PROPRIETARY STUFF FOR PYTHON FILES

void setSeed(){
	srand(time(NULL));
}

//directly taken from compare_strings.py and rewritten in c to do the same thing
float randomGaussian(float mean, float sd){
	float x = ((float) (rand())/(float)(RAND_MAX));
	x = x * 6.2831853;
	float y = (float) (rand())/(float)(RAND_MAX);
	x = cos(x) * sqrt( (-2.0) * log(1.0-y) );
	x = (x * sd) + mean;
	//printf("%s %f %s", "\nhar", x, "\n");
	return x;
}

//directly taken from compare_strings.py and rewritten in c to do the same thing
int getHits(int original[], int testSample[], int originalLen, int sampleLen){
	int hits = 0;
	
	int k;
	for(k = 0; k < 40; k++){
		float x = ((float) (rand()) / (float)(RAND_MAX)) * ((float) (originalLen-1));
		int prev = (int) x;
		int next = prev + 1;
		
		float m = (float)(original[next] - original[prev]);
		float y = m * (x - (float)prev) + (float)original[prev];
		
		float x2 = randomGaussian(x, 0.4);
		int prev2 = (int) x2;
		int next2 = prev2 + 1;
		
		while(next2 >= originalLen || prev2 < 0){
			x2 = randomGaussian(x, 0.4);
			prev2 = (int) x2;
			next2 = prev2 + 1;
		}
		
		float m2 = (float)(original[next2] - original[prev2]);
		float y2 = m2 * (x2 - (float)prev2) + (float) original[prev2];
		
		float mTest = (y2-y)/(x2-x);
		
		int success = 0;
		float sd = originalLen/10.0;
		
		int i;
		for(i = 0; i < 10; i++){
			float nextX = randomGaussian(x, sd);
			int prevN = (int) nextX;
			int nextN = prevN + 1;
			while(nextN >= sampleLen || prevN < 0){
				nextX = randomGaussian(x, sd);
				prevN = (int) nextX;
				nextN = prevN + 1;
			}
			float mN = (float)(testSample[nextN] - testSample[prevN]);
			float yN = mN * (float)(nextX - (float)prevN) + (float)testSample[prevN];
			
			float nextX2 = x2 + nextX - x;
			int prevN2 = (int) nextX2;
			int nextN2 = prevN2 + 1;
			while(nextN2 >= sampleLen || prevN2 < 0){
				nextX2 = randomGaussian(x, sd);
				prevN2 = (int) nextX2;
				nextN2 = prevN2 + 1;
			}
			float mN2 = (float)(testSample[nextN2] - testSample[prevN2]);
			float yN2 = mN2 * (float)(nextX2 - (float)prevN2) + (float)testSample[prevN2];
			
			mN = (yN2 - yN)/(nextX2 - nextX);
			
			if(abs(mTest - mN) < 0.0001){
				hits += 1;
				success = 1;
				break;
			}
			
		}
		if(success == 0){
			continue;
		}
		else{
			int l;
			for(l = 0; l < 19; l++){
				x = x2;
				prev = (int) x;
				next = prev + 1;
				
				m = (float)(original[next] - original[prev]);
				y = m * (x - (float)prev) + (float)original[prev];
				
				x2 = randomGaussian(x, 0.4);
				prev2 = (int) x2;
				next2 = prev2 + 1;
				
				while(next2 >= originalLen || prev2 < 0){
					x2 = randomGaussian(x, 0.4);
					prev2 = (int) x2;
					next2 = prev2 + 1;
				}
				
				m2 = (float)(original[next2] - original[prev2]);
				y2 = m2 * (x2 - prev2) + (float) original[prev2];
				
				mTest = (y2-y)/(x2-x);
				
				success = 0;
				sd = originalLen/10.0;
				
				int j;
				for(j = 0; j < 10; j++){
					float nextX = randomGaussian(x, sd);
					int prevN = (int) nextX;
					int nextN = prevN + 1;
					while(nextN >= sampleLen || prevN < 0){
						nextX = randomGaussian(x, sd);
						prevN = (int) nextX;
						nextN = prevN + 1;
					}
					float mN = (float)(testSample[nextN] - testSample[prevN]);
					float yN = mN * (float)(nextX - (float)prevN) + (float)testSample[prevN];
					
					float nextX2 = x2 + nextX - x;
					int prevN2 = (int) nextX2;
					int nextN2 = prevN2 + 1;
					while(nextN2 >= sampleLen || prevN2 < 0){
						nextX2 = randomGaussian(x, sd);
						prevN2 = (int) nextX2;
						nextN2 = prevN2 + 1;
					}
					float mN2 = (float)(testSample[nextN2] - testSample[prevN2]);
					float yN2 = mN2 * (float)(nextX2 - (float)prevN2) + (float)testSample[prevN2];
					
					mN = (yN2 - yN)/(nextX2 - nextX);
					
					if(abs(mTest - mN) < 0.0001){
						hits += 1;
						success = 1;
						break;
					}
					
				}
				if(success == 0){
					break;
				}
			} 
		}
	}
	hits = (int) (((float)hits) / 2.0);
	//printf("%d", hits);
	return hits;
}

//taken directly from classifier.py, does the same thing but rewritten in C to use C arrays rather than python arrays.
//usees training data to get features and use those for naive bayes classification
//returns an int value. 0 for sentence, 1 for question
int classify(int features[], int input[], int flen, int flen2d, int ilen){
	
	
	int fRowLen = flen2d;
	int fLength = (int)(flen);
	int lenInputFeatures = ilen;
	
	
	float c1Arr[lenInputFeatures];
	float c2Arr[lenInputFeatures];
	float c3Arr[lenInputFeatures];
	float c4Arr[lenInputFeatures];
	
	int c1; // total number of times this feature appears in questions
	int c0; //total number of times this feature does not appear in questions
	int cc1; // total number of times this feature appears in non-questions
	int cc0; // total number of times this feature doees not appear in non-questions
	
	
	float pQuestion = 0.0;
	float pComplement = 0.0;
	int i;
	for(i = 0; i < fLength; i++){
		pQuestion += features[(i * fRowLen) + 1];
		pComplement += (1.0 - features[(i * fRowLen) + 1]);
	}
	pQuestion = log(pQuestion/(float)fLength);
	pComplement = log(pComplement/(float)fLength);
	
	for(i = 0; i < lenInputFeatures; i++){
	
		c1 = 0; // total number of times this feature appears in questions
		c0 = 0; //total number of times this feature does not appear in questions
		cc1 = 0; // total number of times this feature appears in non-questions
		cc0 = 0; // total number of times this feature doees not appear in non-questions
		
		int j;
		for(j = 0; j < fLength; j++){
			
			if(features[(fRowLen * j) + 1] == 1){
				if(features[(fRowLen * j) + i + 2] == 1){
					c1 += 1;
				}
				else{
					c0 += 1;
				}
			}
			else{
				if(features[(fRowLen * j) + i + 2] == 1){
					cc1 += 1;
				}
				else{
					cc0 += 1;
				}
			}
			
		}
		c1Arr[i] = (float)(c1+2)/(float)(c1+2+c0+2);
		c2Arr[i] = (float)(c0+2)/(float)(c1+2+c0+2);
		c3Arr[i] = (float)(cc1+2)/(float)(cc1+2+cc0+2);
		c4Arr[i] = (float)(cc0+2)/(float)(cc1+2+cc0+2);
		
	}
	
	for(i = 0; i < lenInputFeatures; i++){
		if(input[i] == 0){
			pQuestion = pQuestion + log(c2Arr[i]);
			pComplement = pComplement + log(c4Arr[i]);
		}
		else{
			pQuestion = pQuestion + log(c1Arr[i]);
			pComplement = pComplement + log(c3Arr[i]);
		}
	}
	if(pComplement > pQuestion){
		return 0;
	}
	return 1;
}


















//if(intArray == NULL){
//		intArray = malloc(sizeof(int*) * 8);
//	}
//	intArray[intIndex-1] = malloc(sizeof(int) * length);
