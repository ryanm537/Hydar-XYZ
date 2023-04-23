#select a start word, presumably randomly, but stick with that word
#for every possible second word, find the probability of that word coming second
#this is the probability of the second word given the first word, or the probability of the second word coming after the first word
#do this by finding every instance of the first word and finding out what comes after it
#using the top 3 second words, repeat that process as if it were the first
#then, create an array consisting of the first word, and randomly select a second word to append to it using its probability
#that is to say, find all instances of that word and figure out what usually comes after that
#append a third word to the array based on its probability
#repeat this process until the most likely word is an end-of-sentence
#create the array starting from the first word again, using the same probabilities.

import random
import classifier2

#type is either a sentence or a question
def generate(type):

#	f = open("./bots/raye_questions.txt", "r", encoding="utf-8")
#	ff = f.read()
#	ff = ff.lower()
#	#parse words, incliding /n, from the training data
	words = []
#	i = 0
#	while i < len(ff)-1:
#		for j in range(i, len(ff)-1):
#			if ff[j] == " ":
#				words.append(ff[i:j])
#				i=j+1
#			if ff[j:j+1] == "\n":
#				words.append(ff[i:j])
#				words.append("\n")
#				i=j+1
#		i+=1
	f = open("./bots/raye_responses.txt", "r", encoding="utf-8")
	ff = f.read()
	ff = ff.lower()
	i = 0
	while i < len(ff)-1:
		for j in range(i, len(ff)-1):
			if ff[j] == " ":
				words.append(ff[i:j])
				i=j+1
			if ff[j:j+1] == "\n":
				words.append(ff[i:j])
				words.append("\n")
				i=j+1
		i+=1


	#takes three weights and returns a random one of them
	def randomFromThree(p1, p2, p3):
		totalP = p1+p2+p3
		r = random.uniform(0,1)
		if r<(p1/totalP):
			return 0
		if r>(p1/totalP) and r < (p1+p2)/totalP:
			return 1
		if r > (p1 + p2)/totalP:
			return 2

	def findNext(start, first):
		occurrences = []
		for j in range(len(words)-1):
			if (first == True and words[j] == start) or (first == False and j>0 and (words[j-1] + " " + words[j]) == start):
				found = 0
				for k in range(len(occurrences)):
					if occurrences[k][0] == words[j+1]:
						occurrences[k][1] += 1
						found = 1
				if found == 0:
					occurrences.append([words[j+1], 1])
		#take the top three occurrences and use their probabilities to pick a random one
		top3 = [["", 0], ["", 0], ["", 0]]
		for j in occurrences:
			if j[0] != "fuck":
				if j[1] > top3[0][1]:
					top3[2] = top3[1]
					top3[1] = top3[0]
					top3[0] = j
				else:
					if j[1] > top3[1][1]:
						top3[1] = top3[0]
						top3[0] = j
					else:
						if j[1] > top3[2][1]:
							top3[1] = j
						
		#print(top3)
		next = randomFromThree(top3[0][1], top3[1][1], top3[2][1])
		return top3[next][0]

	convergences = []
	
	def generateSentence():
		#for z in range(20):
		#find a random starting word
		i = random.randint(0,len(words)-2)
		while words[i] != "\n":
			i = random.randint(0,len(words)-2)
			if words[i+1] == "fuck":
				i = random.randint(0,len(words)-2)

		startingWord = words[i + 1]

		
		#10 markov chains
		#find every occurrence of the starting word and get the word that comes after
		#repeat occurrences will increase the probability of that occurrence
		chains = []
		for i in range(10):
			chains.append([startingWord])
			prev = startingWord
			next = findNext(startingWord, True)
			while next != "\n":
				chains[i].append(next)
				prev1 = next
				next = findNext(prev + " " + next, False)
				prev = prev1
		
		#turn each chain into a string
		strings = []
		for i in range(len(chains)):
			strings.append("")
			for j in chains[i]:
				strings[i] += j + " "

		#print(strings)

		#count frequency of each chain
		uniqueChains = []
		for i in strings:
			found = 0
			for j in uniqueChains:
				if i[0] != j[0]:
					found = 0
					j[1] += 1
			if found == 0:
				uniqueChains.append([i, 1])
		#print(uniqueChains)

		#find max frequency
		maxFreq = uniqueChains[0]
		for i in uniqueChains:
			if i[1] > maxFreq[1]:
				maxFreq = i

		return(maxFreq[0])
	
	str = generateSentence()
	timeout = 0
	while (timeout < 15 and classifier2.classify(str) != type and len(str) > 50) or (timeout > 15 and len(str) > 50):
		str = generateSentence()
		timeout += 1
	return str
	
#	convergences.append(maxFreq[0])

#find the frequency of each convergence and take the closesd one
#uniqueConvergences = []
#for i in convergences:
#	found = 0
#	for j in uniqueConvergences:
#		if i[0] != j[0]:
#			found = 0
#			j[1] += 1
#	if found == 0:
#		uniqueConvergences.append([i, 1])

#maxFreq = uniqueConvergences[0]
#for i in uniqueConvergences:
#	if i[1] > maxFreq[1]:
#		maxFreq = i
#print(maxFreq[0])








			