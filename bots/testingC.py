import cfunc

import classifier2
import classifier

import time
print(classifier.classify("should we make the eenter text box on har into a text box?"))
print(classifier2.classify("should we make the eenter text box on har into a text box?"))

ms1sum = 0
ms2sum = 0
for i in range(300):
	ms1 = time.time()
	s = (classifier2.classify("should we make the eenter text box on har into a text box?"))
	ms2sum += ((time.time()-ms1)*1000)
print(ms2sum/300)

for i in range(300):
	ms1 = time.time()
	s = (classifier.classify("should we make the eenter text box on har into a text box?"))
	ms1sum += ((time.time()-ms1)*1000)
print(ms1sum/300)

