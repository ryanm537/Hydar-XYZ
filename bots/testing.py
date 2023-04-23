import compare_strings as cs
import classifier

b = "hydar hydar hydar"
s = "hydar"

#x = cs.compareStrings(b, s, 12.6983)

#y = cs.shortStrings(b, s, 12.6675)

#m = 0.9*x + 0.02

#n = 0.9 * y

#a = 0.9 * ((x + y)/2) + 0.008

#print((m+n+a)/3)
print(cs.shortStrings("WHY IS IT SUUS", "sssssssssssssssssssssss yyyyyyyyyyyyyyyyyyyyyyyy", 10))
print(classifier.classify("you are on hydar but not in the hydar board how does that make you feel?"))
