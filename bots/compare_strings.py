import random
import math
#sample = "Once upon a time in a magical world"
#base = "Once upon a time in a magical world"
#base = "when do we go to the magical world"

#a faster algorithm for comparing single words, which does not use any random sampling or markov chains
def singleWord(base, sample):
	n = 0.001
	base = base.lower()
	sample = sample.lower()
	#compare the first and last character
	if base[0]==sample[0] and base[len(base)-1] == sample[len(sample)-1]:
		n+=1
	#add points based on how the characters match up
	n+=1/(max(len(base), len(sample)) - min(len(base), len(sample)) + 1)
	#compare unique characters
	x1 = set(base)
	x2 = set(base)
	xSame = 0
	for i in x1:
		if i in x2:
			xSame+=1;
	n+=(xSame/max(len(x1), len(x2)))
	#return n as a % of its highest possible value
	return n/4.001

#compares characters directly for shorter strings (less than 8 characters)
def shortStrings(base, sample, sdDiv):
	base = base.lower()
	sample = sample.lower()
	#ensure the strings are at least 4 characters (needs at least 4 characters to work)
	while len(base) < 4:
		base = base + base
	while len(sample) < 4:
		sample = sample + sample
	
	#make the strings the same length
	olenb = len(base)
	olens = len(sample)
	for i in range(min(len(base), len(sample)), max(len(base), len(sample))):
		if len(base) == len(sample):
			break
		if len(base)>len(sample):
			sample = sample + sample[i-olens]
		if len(sample) > len(base):
			base = base + base[i-olenb]
	
	#print(base)
	#print(sample)
	
	#random distribution needs to cast to int
	def randomGaussian(mean, standard_dev):
		x = random.uniform(0,1) * 2 * math.pi
		x = math.cos(x) * math.sqrt(-2 * math.log(1-random.uniform(0,1)))
		x = (x * standard_dev) + mean
		if x - int(x) > 0.5:
			return int(x) + 1
		else:
			return int(x)
	
	
#		x = random.uniform(mean-standard_dev*3, mean+standard_dev*3)
#		y = random.uniform(0,0.4/standard_dev)
#		
#		eq = (1/(standard_dev*2.5066)) * (2.71828 ** (-0.5 * (((x-mean)/standard_dev) ** 2)))
#		while y>eq:
#			y = random.uniform(0,0.4/standard_dev)
#			x = random.uniform(mean-standard_dev*3, mean+standard_dev*3)
#			eq = (1/(standard_dev*2.5066)) * (2.71828 ** (-0.5 * (((x-mean)/standard_dev) ** 2)))
#		
#		if x - int(x) > 0.5:
#			return int(x) + 1
#		else:
#			return int(x)
	
	def directComparison(original, testSample):
		
		fTotal = 0
		for j in range(0, 20):
			#choose random start index
			x = random.randint(0,len(original)-1)
			
			failures = 0
			for i in range(0, 5):
				failed = 1
				
				#choose a random candidate index nearby on the other string
				x2 = -1 
				while x2 < 0 or x2 >= len(testSample):
					sd = len(testSample)/sdDiv
					x2 = randomGaussian(x, sd)
				
				if original[x]==testSample[x2]:
						failed = 0
				for k in range(min(x, x2), max(x, x2)+1):
					if original[x]==testSample[k]:
						failed = 0
						break
				
				if failed == 0:
					break
				else:
					failures += 1
			
			fTotal += failures
		return (100-fTotal)/100
	
	n = directComparison(base, sample)
	if n==0:
		n=0.001
	return n

#compares the relationships between characters for longer strings
def compareStrings(base, sample, sdDiv):
	
	base = base.lower()
	sample = sample.lower()
	
	tupleValues = []
	#tupleValues2 = []
	baseTupleValues = []
	
			
	#ensure the strings are at least 4 characters (needs at least 4 characters to work)
	while len(base) < 4:
		base = base + base
	while len(sample) < 4:
		sample = sample + sample
	
	#make the strings the same length
	olenb = len(base)
	olens = len(sample)
	for i in range(min(len(base), len(sample)), max(len(base), len(sample))):
		if len(base) == len(sample):
			break
		if len(base)>len(sample):
			sample = sample + sample[i-olens]
		if len(sample) > len(base):
			base = base + base[i-olenb]
	
	#print(base)
	#print(sample)
	#for i in range(int(len(base)/2)):
		#if i*2<len(base)-1:
			#baseTupleValues.append((ord(base[i*2]) + ord(base[i*2+1]))/2)
		#else:
			#baseTupleValues.append((ord(base[i*2]) + ord(" "))/2)

	#for i in range(int(len(sample)/2)):
		#if i*2<len(sample)-1:
			#tupleValues.append((ord(sample[i*2]) + ord(sample[i*2+1]))/2)
		#else:
			#tupleValues.append((ord(sample[i*2]) + ord(" "))/2)

	for i in range(len(sample)):
		baseTupleValues.append((ord(base[i])))
		tupleValues.append((ord(sample[i])))
		
	#print(tupleValues)
	#print(baseTupleValues)

	#outputs a random value in a range determined by the standard deviation that is more likely to be near the mean
	def randomGaussian(mean, standard_dev):
#		x = random.uniform(mean-standard_dev*3, mean+standard_dev*3)
#		y = random.uniform(0,0.4/standard_dev)
#		
#		eq = (1/(standard_dev*2.5066)) * (2.71828 ** (-0.5 * (((x-mean)/standard_dev) ** 2)))
#		while y>eq:
#			y = random.uniform(0,0.4/standard_dev)
#			x = random.uniform(mean-standard_dev*3, mean+standard_dev*3)
#			eq = (1/(standard_dev*2.5066)) * (2.71828 ** (-0.5 * (((x-mean)/standard_dev) ** 2)))
		x = random.uniform(0,1) * 2 * math.pi
		x = math.cos(x) * math.sqrt(-2 * math.log(1-random.uniform(0,1)))
		x = (x * standard_dev) + mean
		
		return x


	#does something similar to hill climbing using something similar to metropolis-hastings
	#basically, it takes the tuple arrays as graphs and walks through starting from a random point
	#each next point is randomly chosen using a gaussian distribution near the previous point
	#it then tries to find if the same slope created by the next point and current point is found nearby in the other graph
	#if it fails to find it, it will try 9 more times before abandoning that markov chain
	#if it does find it, it will continue along that path, ideally picking up more hits along the way
	#in order to consistenly succeed the graphs will need to be quite similar (proportional)
	def hillClimbComparison(original, testSample):
		# repeat process 40 times for 40 markov chains
		# track the failure rate of each chain
		
		hits = 0
		for k in range(0, 40):
			#P is the original (base)
			#f is the sample
			#pick a random point "x" on P
			x = random.uniform(0,len(original)-1)

			#find the point before and point after to compute a slope
			#use this to find the y coordinate of x
			prev = math.floor(x)
			next = math.ceil(x)
			m = original[next] - original[prev]
			y = m * (x - prev) + original[prev]

			#use the gaussian function to find a nearby random candidate
			x2 = randomGaussian(x, 0.4)

			#find the y coordinate of x2
			prev2 = math.floor(x2)
			next2 = math.ceil(x2)
			while next2 >= len(original) or prev2 < 0:
				x2 = randomGaussian(x, 0.4)
				prev2 = math.floor(x2)
				next2 = math.ceil(x2)
			m2 = original[next2]-original[prev2]
			y2 = m2 * (x2 - prev2) + original[prev2]

			#find the slope between x1 and x2
			mTest = (y2-y)/(x2-x)

			#see if this slope exists within a gaussian-random generated distance from x2 on f
			#test 10 different times to see if it can find a match 
			#track failures
			success = 0
			sd = len(original)/sdDiv
			for i in range(0,10):
				nextX = randomGaussian(x, sd)
				#calculate slope at next point
				nextX2 = x2 + nextX - x
				prevN = math.floor(nextX)
				nextN = math.ceil(nextX)
				while nextN >= len(testSample) or prevN < 0:
					nextX = randomGaussian(x, sd)
					prevN = math.floor(nextX)
					nextN = math.ceil(nextX)
				mN = testSample[nextN]-testSample[prevN]
				yN = mN * (nextX - prevN) + testSample[prevN]
				prevN2 = math.floor(nextX2)
				nextN2 = math.ceil(nextX2)
				while nextN2 >= len(testSample) or prevN2 < 0:
					nextX2 = randomGaussian(x, sd)
					prevN2 = math.floor(nextX2)
					nextN2 = math.ceil(nextX2)
				mN2 = testSample[nextN2]-testSample[prevN2]
				yN2 = mN2 * (nextX2 - prevN2) + testSample[prevN2]
				
				mN = (yN2 - yN) / (nextX2 - nextX)
				
				if abs(mTest-mN) < 0.001:
					hits += 1
					success = 1
					break
			#if, after 10 tries, a successful match has not been found, this markov chain will end
			#use continue to try another chain entirely
			#else, keep iterating through the current chain
			if success == 0:
				continue
			else:
				#repeat the process for 20 more points. Replace x with x2
				for j in range(0, 19):
					x = x2
					prev = math.floor(x)
					next = math.ceil(x)
					m = original[next]-original[prev]
					y = m * (x - prev) + original[prev]
					
					#use the gaussian function to find a nearby random candidate
					x2 = randomGaussian(x, 0.4)
					
					#find the y coordinate of x2
					prev2 = math.floor(x2)
					next2 = math.ceil(x2)
					while next2 >= len(original) or prev2 < 0:
						x2 = randomGaussian(x, 0.4)
						prev2 = math.floor(x2)
						next2 = math.ceil(x2)
					m2 = original[next2]-original[prev2]
					y2 = m2 * (x2 - prev2) + original[prev2]
					
					#find the slope between x1 and x2
					mTest = (y2-y)/(x2-x)
					
					#test candidate
					success = 0
					for i in range(0,10):
						nextX = randomGaussian(x, sd)
						#calculate slope at next point
						nextX2 = x2 + nextX - x
						prevN = math.floor(nextX)
						nextN = math.ceil(nextX)
						while nextN >= len(testSample) or prevN < 0:
							nextX = randomGaussian(x, sd)
							prevN = math.floor(nextX)
							nextN = math.ceil(nextX)
						mN = testSample[nextN]-testSample[prevN]
						yN = mN * (nextX - prevN) + testSample[prevN]
						prevN2 = math.floor(nextX2)
						nextN2 = math.ceil(nextX2)
						while nextN2 >= len(testSample) or prevN2 < 0:
							nextX2 = randomGaussian(x, sd)
							prevN2 = math.floor(nextX2)
							nextN2 = math.ceil(nextX2)
						mN2 = testSample[nextN2]-testSample[prevN2]
						yN2 = mN2 * (nextX2 - prevN2) + testSample[prevN2]
						
						mN = (yN2 - yN) / (nextX2 - nextX)
						
						if abs(mTest-mN) < 0.001:
							hits += 1
							success = 1
							break
					#if, after 10 tries, a successful match has not been found, this markov chain will end
					#use break to exit the current chain and try another chain entirely
					if success == 0:
						break
			#hits should now contain all the hits found
		return hits
	hTotal = hillClimbComparison(baseTupleValues, tupleValues)
	
	
	#stuff to make the number into a probability between 0 and 1
	n = (hTotal)/30
	if n == 0:
		n= 0.001
	#n = (1000/(n-4000) - 0.2) * 1.25
	if n > 1 or n < 0:
		n=1.0
	#print(n)
	return n