from ctypes import *
#cfile = "./cstuff.dll"
cfile = "./bots/cstuff.dll"
cFunctions = CDLL(cfile)
#class array:
#	def __init__(self, arg):
#		self.length = 0
#		self.cIndex = 0
#		#initialize array with length
#		if type(arg) == list:
#			#support for creating a c array from a python array
#			if type(arg[0]) != list:
#				#check if  float array or not
#				if type(arg[0]) == int:
#					self.length = len(arg)
#					self.cIndex = cFunctions.array(self.length)
#					values = (c_int * self.length)(*arg)
#					self.length2d = 0
#					cFunctions.createFromExisting(values, self.length, self.cIndex, self.length2d)
#					
#				if type(arg[0]) == float:
#					self.length = len(arg)
#					self.cIndex = cFunctions.farray(self.length)
#					values = (c_float * self.length)(*arg)
#					self.length2d = 0
#					cFunctions.fcreateFromExisting(values, self.length, self.cIndex, self.length2d)
#			else:
#				#support for 2d array
#				#check if float  array or int array
#				if type(arg[0][0]) == int:
#					self.length = len(arg) * len(arg[0])
#					self.cIndex = cFunctions.array(self.length)
#					x = [0] * self.length
#					for i in range(len(arg)):
#						x[i*len(arg[i]):(i*len(arg[i]) + len(arg[i]))] = arg[i]
#					values = (c_int * self.length)(*x)
#					self.length2d = len(arg[0])
#					cFunctions.createFromExisting(values, self.length, self.cIndex, self.length2d)
#					
#				if type(arg[0][0]) == float:
#					self.length = len(arg) * len(arg[0])
#					self.cIndex = cFunctions.farray(self.length)
#					x = [0] * self.length
#					for i in range(len(arg)):
#						x[i*len(arg[i]):(i*len(arg[i]) + len(arg[i]))] = arg[i]
#					values = (c_float * self.length)(*x)
#					self.length2d = len(arg[0])
#					cFunctions.fcreateFromExisting(values, self.length, self.cIndex, self.length2d)
#				
#		else:
#			self.length = arg
#			self.cIndex = cFunctions.array(self.length)
#	
#	#int functions
#	def insert(self, index, value):
#		cFunctions.insert(index, value, self.cIndex)
#	
#	def get(self, index):
#		return cFunctions.get(index, self.cIndex)
#		
#	def append(self, value):
#		if type(value) == list:
#			values = (c_int * len(value))(*value)
#			cFunctions.appendList(values, self.cIndex, len(value))
#		else:
#			cFunctions.append(value, self.cIndex)
#		
#	def sMult(self, s):
#		cFunctions.smult(s, self.cIndex)
#		
#	def getLen(self):
#		return cFunctions.getLength(self.cIndex)
#	
#	#float functions
#	def finsert(self, index, value):
#		cFunctions.finsert(index, value, self.cIndex)
#	
#	def fget(self, index):
#		return cFunctions.fget(index, self.cIndex)
#		
#	def fappend(self, value):
#		cFunctions.fappend(value, self.cIndex)
#		
#	def fsMult(self, s):
#		cFunctions.fsmult(s, self.cIndex)
#		
#	def fgetLen(self):
#		return cFunctions.fgetLength(self.cIndex)
#	
#	#other functions
#	def getCIndex(self):
#		return self.cIndex;
	
def classify(inputFeatures, features):
	flen = len(features)
	flen2d = len(features[0])
	ilen = len(inputFeatures)

	inputFeat = (c_int * len(inputFeatures))(*inputFeatures)
	x = [0] * len(features)
	for i in range(len(features)):
		x[i*len(features[i]):(i*len(features[i]) + len(features[i]))] = features[i]
	feat = (c_int * len(x))(*x)
	return cFunctions.classify(feat, inputFeat, flen, flen2d, ilen)
		
def getHits(original, sample):
	cFunctions.setSeed()
	oVal = (c_int * len(original))(*original)
	sVal = (c_int * len(sample))(*sample)
	return cFunctions.getHits(oVal, sVal, len(original), len(sample))
		
	
	
