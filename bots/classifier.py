#probability of Y given a list of features is the probability of Y occcuring times the product of the probability of each feature occurring given Y
#the probability of a feature occurring given Y is (the number of times that feature occurred in things classified as Y) over (the sum of the number of times that feature occurred in instances of Y for each possible value of that feature)
import compare_strings2 as cs
import math


def classify(str):
	#first, open the file

	f = open("./bots/training_data.txt", "r", encoding="utf-8")
	ff = f.read()
	ff = ff.lower()
	ff = ff.replace("'", "")
	# first value in a tuple in data is the type. 1 for questions, 0 for statements.
	#second value is the text contents of the sentence
	data = []
	for i in range(len(ff)-13):
		if ff[i:i+11] == "question --":
			i=i+12
			for j in range(i, len(ff)-1):
				if ff[j:j+1]=="\n":
					if ff[i:j]!="":
						data.append([ff[i:j], 1])
					i = j + 2
					break
		if ff[i:i+12] == "statement --":
			i=i+13
			for j in range(i, len(ff)-1):
				if ff[j:j+1]=="\n":
					if ff[i:j]!="":
						data.append([ff[i:j], 0])
					i = j + 2
					break

	#print(data)
	
	
	#functions to determine if a word resembles certain other words
	def questionStarter(str):
		questionStarters = ["who", "what", "where", "when", "why", "how", "thoughts", "wtf", "wth", "tf"]
		x = 0
		for i in questionStarters:
			if cs.singleWord(str, i) > 0.75:
				x = 1
				break
		return x

	def auxiliaryVerb(str):
		verbs = ["are", "can", "do", "will", "is", "arent", "cant", "cannot", "did", "doesnt", "dont", "does", "wont", "isnt", "was", "wasnt", "am"]
		x = 0
		for i in verbs:
			if cs.singleWord(str, i) > 0.75:
				x = 1
				break
		return x
	
	
	def starterAndVerb(str):
		questionStarters = ["who", "what", "where", "when", "why", "how", "thoughts", "wtf", "wth", "tf"]
		verbs = ["are", "can", "do", "will", "is", "arent", "cant", "cannot", "did", "doesnt", "dont", "does", "wont", "isnt", "was", "wasnt", "am"]
		for i in range(len(questionStarters)):
			for j in range(len(verbs)):
				phrase = questionStarters[i] + " " + verbs[j]
				if phrase in str:
					return 1
		return 0
	#for each sentence, store its features in an array
	#each array within the 2d features aray will contain the string, a binary value indicating if it is a question, and a binary value of each feature

	def getFeatures(str):
		#features
		startsQ = 0 #starts with a question word such as who or what
		containsQV = 0 # contains a question verb such as can or did
		containsQS = 0 # contains a question starter such as who or what
		endsQ = 0 # ends with a question mark
		endsP = 0 # ends with a period or comma
		startsI = 0 # starts with the letter "i"
		containsSandV = 0 #has a starter and a verb adjacent in it, like "what is" or "how do"
		
		if len(str) > 1 and str[0:2] == "i ":
			startsI = 1
		for k in range(len(str)):
			if str[k] == " ":
				if startsQ == 0 and (questionStarter(str[0:k]) == 1 or auxiliaryVerb(str[0:k]) == 1):
					startsQ = 1
				l = k+1
				while l < len(str):
					if containsQS == 0 and str[l] == " " and questionStarter(str[k+1:l]) == 1:
						containsQS = 1
					if containsQV == 0 and str[l] == " " and auxiliaryVerb(str[k+1:l]) == 1:
						containsQV = 1
					l+=1
		if str[len(str)-1] == "?":
			endsQ = 1
		if str[len(str)-1] == "." or str[len(str)-1] == ",":
			endsP = 1
		if containsQV == 1 and containsQS == 1:
			containsSandV = starterAndVerb(str)
		
		return [startsQ, containsQV, containsQS, endsQ, endsP, startsI, containsSandV]

	features = []
	for i in data:
		#loop through the string portion of i and for each string in i and set the features
		#add an array with the features of i[0] to the features array, as well as whetheer i[0][0] is a question
		f = getFeatures(i[0])
		x = [i[0], i[1]]
		for j in f:
			x.append(j)
		features.append(x)

	#print(features)

	#compute features for input
	#then, calculate the probability of the input being a question given each of those features
	inp = str

	#pQueston = P(y = 1)
	#pComplement = P(y = 0)
	pQuestion = 0
	pComplement = 0
	for i in features:
		pQuestion += i[1]
		pComplement += (1 - i[1])
	pQuestion = math.log(pQuestion/len(features))
	pComplement = math.log(pComplement/len(features))

	#input features is the feature array for the input and complement is the opposite
	inputFeatures = getFeatures(inp)
	#print(inputFeatures)
	inputComplement = inputFeatures
#	for i in inputComplement:
#		if i == 1:
#			i = 0
#		else:
#			i = 1

	c1Array = [] # probabilies that each feature occurs given a question
	c0Array = [] # probabilities that each feature does not occur given a question
	cc1Array = [] # probabilities that each feature occurs given a non-question
	cc0Array = [] # probabilities that each feature does not occur given a non-question
	#print(features)
	for i in range(len(inputFeatures)):
		#for each feature, compute c, or the number of instances from the training that this feature occured
		
		
		c1 = 0 # total number of times this feature appears in questions
		c0 = 0 # total number of times this feature does not appear in questions
		cc1 = 0 # total number of times this feature appears in non-questions
		cc0 = 0 # total number of times this feature doees not appear in non-questions
		
		for j in features:
			if j[1] == 1:
				if 1 == j[i+2]:
					c1 += 1
				else:
					c0 += 1
			else:
				if 1 == j[i+2]:
					cc1 += 1
				else:
					cc0 += 1
			#j=j+1
		#print(c1)
		#print(c0)
		#print(cc1)
		#print(cc0)
		c1Array.append((c1+2)/(c1+2+c0+2))
		c0Array.append((c0+2)/(c1+2+c0+2))
		cc1Array.append((cc1+2)/(cc1+2+cc0+2))
		cc0Array.append((cc0+2)/(cc1+2+cc0+2))
	for i in range(len(inputFeatures)):
		if inputFeatures[i] == 0:
			pQuestion = pQuestion + math.log(c0Array[i])
			pComplement = pComplement + math.log(cc0Array[i])
		else:
			pQuestion = pQuestion + math.log(c1Array[i])
			pComplement = pComplement + math.log(cc1Array[i])
	
	if pComplement > pQuestion:
		return("sentence")
	else:
		return("question")

#print(inputFeatures)
















