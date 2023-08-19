# -*- coding: utf-8 -*-
# hdyar
# WRITTEN BY SOCKS_M and GLENN M

import os, time
import urllib.request, json
def refresh():
    rare=["forceful","strong","godly","zealous","demonic","hurtful","superior","keen"]
    skeleman=["cer sword","reaper scyt","summoning ring"]
    if(os.path.exists('auctions.txt')):
        os.remove('auctions.txt')
    data = json.loads(urllib.request.urlopen("https://api.hypixel.net/skyblock/auctions?page=0").read().decode())
    if(not data['success']):
        exit(0)
    auctions = []
    for i in range(data['totalPages']):
        e = json.loads(urllib.request.urlopen("https://api.hypixel.net/skyblock/auctions?page="+str(i)).read().decode())
        ms = int(time.time()*1000)
        for auction in e['auctions']: 
            if "water hydra" in auction['item_name'].lower():
                for x in rare:
                    if x in auction['item_name'].lower():
                        v=ign(auction['uuid'])
                        print("RARE HYDAR HEAD AASJdhlkajfhlksd /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper().replace("HYDRA","HYDAR")+")!!!!!!!!")
            for x in skeleman:
                if x in auction['item_name'].lower():
                    if "water hydra" in auction['item_lore'].lower():
                        v=ign(auction['uuid'])
                        print("HDYAR SOULLL /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper()+")!!!!!!!!")
                    if "the emperor" in auction['item_lore'].lower():
                        v=ign(auction['uuid'])
                        print("eMP SOULL(not hdyar) /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper()+")!!!!!!!!")
                    if "great white" in auction['item_lore'].lower():
                        v=ign(auction['uuid'])
                        print("gw soul(not hdar) /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper()+")!!!!!!!!")
            
            if 'bin' in auction.keys() and auction['end']>ms:
            # print(auction) and (auction['category']!="weapon" and (auction['category']!="consumables" or ("Worm" in auction["item_name"])) and (auction['category']!='armor' or ("divan" in auction['item_name'].lower()) or ("hydra" in auction['item_name'].lower())) and not ("cake soul" in auction["item_lore"].lower()) and not ("SPECIAL" in auction["tier"]) and ("COMMON" !=auction["tier"]) and ((not "]" in auction["item_name"].lower()) or "ammonite" in auction["item_name"].lower()) and not("skin" in auction["item_name"].lower())
                auctions.append([auction['item_name'].replace("]","").replace("[",""),auction['starting_bid']])
    hydar = open('auctions.txt','w', encoding="utf-8")
    print(auctions,file=hydar)
    hydar.close()
    if(os.path.exists('bazaar.txt')):
        os.remove('bazaar.txt')
    data = json.loads(urllib.request.urlopen("https://api.hypixel.net/skyblock/bazaar").read().decode())
    if(not data['success']):
        exit(0)
    prices = []
    for key in data['products'].keys():
        a=data['products'][key]
        # print(p)
        prices.append([a['product_id'],a["buy_summary"][0]["pricePerUnit"] if len(a["buy_summary"])>0 else 0.1,a["sell_summary"][0]["pricePerUnit"] if len(a["sell_summary"])>0 else 0.1])
    f = open('bazaar.txt','w', encoding="utf-8")
    print(prices,file=f)
    f.close()

n1=[]
n2=[]
cache=[]
cache1=[]
cache2=[]
def lbin(name):
    global n1
    for (q,v) in cache:
        if q==name:
            return v
    if(n1==[]):
        f = open("auctions.txt", "r", encoding='utf-8')
        x=f.read()
        f.close()
        n1=eval(x)
    # print(n)
    v=0
    lb1=-1
    lb2=-1
    lb3=-1
    for q in n1:
        if name.lower() in q[0].lower():
            # print(q[0])
            if(q[1]<lb1 or lb1==-1):
                lb3=lb2
                lb2=lb1
                lb1=q[1]
            elif q[1]<lb2 or lb2==-1:
                lb3=lb2
                lb2=q[1]
            elif q[1]<lb3 or lb3==-1:
                lb3=q[1]
    if(lb1==-1):
        print("warning: no auctions found for \""+name+"\"")
        v= -1
    elif(lb2==-1):v= lb1
    elif(lb3==-1):v= (lb1+lb2)/2
    else: v= (lb1+lb2+lb3)/3
    cache.append((name,v))
    return v
def bz(id):
    global n2
    for (q,v) in cache1:
        if q==id:
            return v
    t=False
    if("goblin" in id):
        t=True
    # instabuy price(for the actual resulting forge item u should use instasell price)
    if n2==[]:
        f = open('bazaar.txt','r', encoding='utf-8')
        x=f.read()
        f.close()
        n2=eval(x)
    v=-1
    for q in n2:
        if id.lower().replace("_"," ") in q[0].lower().replace("_"," ") and ("enchanted" in q[0].lower()) == ("enchanted" in id.lower()) and (("block" in q[0].lower()) == ("block" in id.lower())) and ("refined" in q[0].lower()) == ("refined" in id.lower()):
            if(not t):
                v=q[1]
            else:
                if((("red" in q[0].lower()) == ("red" in id.lower())) and (("yellow" in q[0].lower()) == ("yellow" in id.lower())) and (("green" in q[0].lower()) == ("green" in id.lower())) and (("blue" in q[0].lower()) == ("blue" in id.lower()))):
                    v=q[1]
    cache1.append((id,v))
    if v==-1:
        print("warning: no bz data found for \""+id+"\"")
    return v
def bzsell(id):
    global n2
    for (q,v) in cache2:
        if q==id:
            return v
    t=False
    if("goblin" in id):
        t=True
    # instasell price(for things like power crystal)
    if n2==[]:
        f = open('bazaar.txt','r', encoding='utf-8')
        x=f.read()
        f.close()
        n2=eval(x)
    v=-1
    for q in n2:
        if id.lower().replace("_"," ") in q[0].lower().replace("_"," ") and ("enchanted" in q[0].lower()) == ("enchanted" in id.lower()) and (("block" in q[0].lower()) == ("block" in id.lower())) and ("refined" in q[0].lower()) == ("refined" in id.lower()):
            if(not t):
                v=q[2]
            else:
                if((("red" in q[0].lower()) == ("red" in id.lower())) and (("yellow" in q[0].lower()) == ("yellow" in id.lower())) and (("green" in q[0].lower()) == ("green" in id.lower())) and (("blue" in q[0].lower()) == ("blue" in id.lower()))):
                    v=q[2]
    cache2.append((id,v))
    if v==-1:
        print("warning: no bz data found for \""+id+"\"")
    return v
# example usages of the functions
# recommended use of refresh(runs every time the calc is run, but not if used in last 5 min)
if (not os.path.exists('auctions.txt')) or (((int(time.time()))-os.path.getmtime("auctions.txt"))>3600*24):
    refresh()
    

quick_forge_bonus = 1

hydar1=[
    ["God Potion", (lbin("god pot") / 1500)],
    ["Kat Flower", (lbin("kat flower") / 500)],
    ["Heat Core", (lbin("heat core") / 3000)],
    ["Hyper Catalyst Upgrade", (lbin("Hyper catalyst upgrade") / 300)],
    ["Ultimate Carrot Candy Upgrade", (lbin("Ultimate Carrot Candy Upgrade") / 8000)],
    ["Colossal Experience Bottle Upgrade", (lbin("Colossal Experience Bottle Upgrade") / 1200)],
    ["Jumbo Backpack Upgrade", (lbin("Jumbo Backpack Upgrade") / 4000)],
    ["Minion Storage X-pender", (lbin("Minion Storage X-pender") / 1500)],
    ["Hologram", (lbin("Hologram") / 2000)],
    ["Dungeon Sack", (0)],
    ["Builder's Wand", (lbin("builder's wand") / 12000)],
    ["Block Zapper",  (lbin("block zapper") / 5000)],
    ["Bits Talisman", (lbin("Bits talisman") / 15000)],
    ["Rune Sack", (0)],
    ["Autopet Rules 2-Pack", (lbin("Autopet Rules 2-Pack") / 21000)],
    ["Kismet Feather", (lbin("Kismet feather") / 1350)],
    ["Expertise",  (lbin("expertise") / 4000)],
    ["Compact", (lbin("compact") / 4000)],
    ["Cultivating", (lbin("Cultivating") / 4000)],
    ["Speed Enrichment", (lbin("Speed Enrichment") / 5000)],
    ["Intelligence Enrichment", (lbin("Intelligence Enrichment") / 5000)],
    ["Critical Enrichment", (lbin("Critical Enrichment") / 5000)],
    ["Strength Enrichment", (lbin("Strength Enrichment") / 5000)],
    ["Defence Enrichment", (lbin("Defence Enrichment") / 5000)],
    ["Health Enrichment", (lbin("Health Enrichment") / 5000)],
    ["Magic Find Enrichment", (lbin("Magic Find Enrichment") / 5000)],
    ["Ferocity Enrichment", (lbin("Ferocity Enrichment") / 5000)],
    ["Sea Creature Chance Enrichment", (lbin("Sea Creature Chance Enrichment") / 5000)],
    ["Attack Speed Enrichment", (lbin("Attack Speed Enrichment") / 5000)],
    ["Accessory Enrichment Swapper", (lbin("Accessory Enrichment Swapper") / 200)]
]

hydar1=[[a,round(b)] for [a,b] in hydar1]

hydarA=sorted(hydar1,key=lambda x: x[1])[::-1]

print("\nBITS:")
for [a,b] in hydarA:print(""+a+" at "+str(b))


