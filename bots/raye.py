#takes a string as input and matches it against a set of example questions to come up with a response.
#compares the top potential responses with each other to find if an answer among the top responses is better than the absolute top one
#raye uses data from conversations on hydar to come up with her answers

import compare_strings2 as cs
import random
import math
import threading
import sys
import generate_text
import classifier2
lock = threading.Lock()
inp = sys.argv[1:len(sys.argv)]
inp = " ".join(inp)
#print(inp)
#previous_messages = ["yo", "hydar", "whats the weather like", "i like trains", "who are you", "what is skypixel hyblock", "why does hypixel skyblock exist"]
#responses = ["yo", "hydar", "idk", "dude same", "huh?", "reference to the popular 2018 video game among us", "im not sure either"]
f = open("./bots/raye_questions.txt", "r")
ff = f.read()

previous_messages = []
i = 0
while i < len(ff)-1:
	for j in range(i, len(ff)):
		if ff[j : j+1] == "\n":
			previous_messages.append(ff[i:j])
			i = j+1
	i+=1
f = open("./bots/raye_responses.txt", "r")
ff = f.read()

responses = []
i = 0
while i < len(ff)-1:
	for j in range(i, len(ff)):
		if ff[j : j+1] == "\n":
			responses.append(ff[i:j])
			i = j+1
	i+=1



#returns the probability that message2 is correct assuming message1 is correct
#in other words, compares message 2 to message1
def createProbability(message1, message2):
	#very quickly, without running any of the more advanced algorithms, we can just check if the strings are equal
	if message1 == message2:
		return 1.0
	else:
		#otherwise, use the following algorithms
		#if the length of the string is less than 8, it is better to just compare the individual characters using shortstrings
		if(max(len(message1), len(message2)) < 8):
		#		n = 0.0
		#		for i in range(min(len(message1), len(message2))):
		#			if message1[i] == message2[i]:
		#				n=n+1.0
		#		
		#		if n == 0.0:
		#			m= 0.001
		#		else:
		#			m= n/max(len(message1), len(message2))
			
			return (cs.shortStrings(message1, message2, 10))
		else:
			#otherwise, use the algorithm in compare_strings.py
			x = cs.compareStrings(message1, message2, 10)
			y = cs.shortStrings(message1, message2, 10)
			#print(message1 + " " + str(x) + " " + message2 + " " + str(y))
			return (x+y)/2


#probability that the corresponding meessage from previous_messages is the right message
#for now, it's also the probability that it's response is correct
lp = len(previous_messages) #lp is going to be used a bit, its just the length of the previous messages array
p_match = [0] * len(previous_messages)
#populate p_match with base probabilities
#use 4 threads to make this a bit faster
def populatePMatch(initial, end):
	global p_match
	for i in range(initial, end):
		with lock:
			p_match[i] = (createProbability(inp, previous_messages[i]))

t1 = threading.Thread(target = populatePMatch, args=(0, int(lp/4), ))
t2 = threading.Thread(target = populatePMatch, args=(int(lp/4), (2 * int(lp/4)), ))
t3 = threading.Thread(target = populatePMatch, args=((2 * int(lp/4)), (3 * int(lp/4)), ))
t4 = threading.Thread(target = populatePMatch, args=((3 * int(lp/4)), lp, ))
t1.start()
t2.start()
t3.start()
t4.start()
t1.join()
t2.join()
t3.join()
t4.join()

#print(p_match)

#enumerate p_match like this because enumerate doesn't really give what i want
en_p_match = [0] * lp
for i in range(lp):
	en_p_match[i] = ([i, p_match[i]])
random.shuffle(en_p_match)
#sort enumerated p match based on probability
en_p_match.sort(reverse = True, key=lambda en_p_match:en_p_match[1])
#print(en_p_match)

#get the reponses that give the highest probabilities and put them in an array
#responses within an error range of 0.1 from the max are allowed in maxResp
maxResp = []
count = 0 # cap the number of things possible in maxResp
for i in en_p_match:
	#threshold for max resp is 0.57, and it has to be within 0.1 of the max from en p match.
	#for time saving, limit to 28 possibilities
	if count < 28 and en_p_match[0][1] - i[1] < 0.1 and i[1] >= 0.67:
		maxResp.append(responses[i[0]])
	count += 1

#print(maxResp)

#threshold for highest en p match value is 0.67
if len(maxResp) > 0 and en_p_match[0][1] > 0.67:
	#find the condidional probability of each sentence in maxResp given that each of the others is correct
	#using bayes theorem, P(maxResp[x]|maxResp[i], ... maxResp[n]) should be proportional to P(maxResp[x]) * product i->n (P(maxResp[i]|maxResp[x]))
	#however, I will instead use sum of logs to reduce underflow

	newProbabilities = [0] * len(maxResp)

	#use 4 threads to do this if there are more than 4 values in maxResp
	if len(maxResp) >= 4:
		def condProbs(initial, end):
			global newProbabilities
			for i in range(initial, end):
				newProbability = math.log(en_p_match[i][1])
				for j in range(len(maxResp)):
					if i!=j:
						newProbability += math.log(createProbability(maxResp[i], maxResp[j]))
				with lock:
					newProbabilities[i] = (newProbability)
				

		t1 = threading.Thread(target = condProbs, args=(0, int(len(maxResp)/4), ))
		t2 = threading.Thread(target = condProbs, args=(int(len(maxResp)/4), (2 * int(len(maxResp)/4)), ))
		t3 = threading.Thread(target = condProbs, args=((2 * int(len(maxResp)/4)), (3 * int(len(maxResp)/4)), ))
		t4 = threading.Thread(target = condProbs, args=((3 * int(len(maxResp)/4)), len(maxResp), ))
		t1.start()
		t2.start()
		t3.start()
		t4.start()
		t1.join()
		t2.join()
		t3.join()
		t4.join()
	else:
		for i in range(len(maxResp)):
			newProbability = math.log(en_p_match[i][1])
			for j in range(len(maxResp)):
				if i!=j:
					newProbability += math.log(createProbability(maxResp[i], maxResp[j]))
			with lock:
				newProbabilities[i] = (newProbability)

	#print(newProbabilities)

	#find argmax of newProbabilities
	maxNewP = [0, newProbabilities[0]]
	for i in enumerate(newProbabilities):
		if i[1] > maxNewP[1]:
			maxNewP = i

	print(maxResp[maxNewP[0]])

else:
	
	starters = ["Hydar, also ", "I mean hydar but like ", "Hydar, but also ", "Hydar, and also "]
	str = starters[random.randint(0,len(starters)-1)]
	#if classifier.classify(previous_messages[maxNewP[0]]) == "question":
	if classifier2.classify(inp) == "question":
		str += "i dont know"#generate_text.generate("sentence")
	else:
		newStr = generate_text.generate("question")
		str += "ok"#newStr[0:len(newStr)-1] + "?"
		#print(previous_messages[maxNewP[0]])
	print(str)

	
















































