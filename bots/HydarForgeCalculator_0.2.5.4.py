# -*- coding: utf-8 -*-
# hdyar
import os, time
import urllib.request, json
def ign(uuid):
    n = json.loads(urllib.request.urlopen("https://api.mojang.com/user/profiles/"+str(uuid)+"/names").read().decode())
    v=0
    a=""
    for q in n:
        if "changedToAt" in q.keys():
            if(q['changedToAt']>v):
                v=q['changedToAt']
                a=q['name']
    if a=="":
        for q in n:
            if "changedToAt" not in q.keys():
                a=q['name']
    return a
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
                        v=auction['uuid']
                        print("RARE HYDAR HEAD AASJdhlkajfhlksd /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper().replace("HYDRA","HYDAR")+")!!!!!!!!")
            for x in skeleman:
                if x in auction['item_name'].lower():
                    if "water hydra" in auction['item_lore'].lower():
                        v=auction['uuid']
                        print("HDYAR SOULLL /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper()+")!!!!!!!!")
                    if "the emperor" in auction['item_lore'].lower():
                        v=auction['uuid']
                        print("eMP SOULL(not hdyar) /viewauction "+v+" "+str(auction['starting_bid'])+"("+auction["item_name"].upper()+")!!!!!!!!")
                    if "great white" in auction['item_lore'].lower():
                        v=auction['uuid']
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
        return bz(name)
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
    # Refine Ore
    ["Refined Diamond",(bzsell("refined diamond") - bz("enchanted diamond block") * 2) / (6 * quick_forge_bonus)],
    ["Refined Mithril", (bzsell("refined mithril") - bz("enchanted mithril") * 160) / (6 * quick_forge_bonus)],
    ["Refined Titanium", (bzsell("refined titanium") - bz("enchanted titanium") * 16) / (12 * quick_forge_bonus)],
    ["Fuel Tank", (lbin("fuel tank") - bz("enchanted coal block") * 2) / (10 * quick_forge_bonus)],
    ["Bejeweled Handle", (lbin("bejeweled handle") - lbin("glacite jewel") * 3) / (0.5 * quick_forge_bonus)],
    ["Drill Engine", (lbin("drill engine") - (bz("enchanted iron block") + bz("enchanted redstone block") * 3 + lbin("golden plate") + bz("treasurite") *10 + bz("refined diamond"))) / (30 * quick_forge_bonus)],
    ["Golden Plate", (bzsell("golden plate") - (bz("enchanted gold block") * 2 + bz("glacite jewel") * 5 + bz("refined diamond"))) / (6 * quick_forge_bonus)],
    ["Mithril Plate", (bzsell("mithril plate") - (bz("refined mithril") * 5 + bz("golden plate") + bz("enchanted iron block") + bz("refined titanium"))) / (18 * quick_forge_bonus)],
    ["Gemstone Mixture", (lbin("gemstone mixture") - (bz("fine jade") * 4 + bz("fine amber") * 4 + bz("fine amethyst") * 4 + bz("fine sapphire") * 4 + bz("sludge juice") * 320)) / (4 * quick_forge_bonus)],
    ["Perfect Jasper", (bzsell("perfect jasper") - bz("flawless jasper") * 5) / (20 * quick_forge_bonus)],
    ["Perfect Ruby", (bzsell("perfect ruby") - bz("flawless ruby") * 5) / (20 * quick_forge_bonus)],
    ["Perfect Jade", (bzsell("perfect jade") - bz("flawless jade") * 5) / (20 * quick_forge_bonus)],
    ["Perfect Sapphire (inculding robot part)", (bzsell("perfect sapphire") - (bz("flawless sapphire") * 5 + lbin("control switch") + lbin("ftx 3070") + lbin("electron transmitter") + lbin("robotron reflector") + lbin("synthetic heart") + lbin("superlite motor"))) / (20 * quick_forge_bonus)],
    ["Perfect Amber", (bzsell("perfect amber") - bz("flawless amber") * 5) / (20 * quick_forge_bonus)],
    ["Perfect Topaz", (bzsell("perfect topaz") - bz("flawless topaz") * 5) / (20 * quick_forge_bonus)],
    ["Perfect Amethyst", (bzsell("perfect amethyst") - bz("flawless amethyst") * 5) / (20 * quick_forge_bonus)]]

    # Item Casting
hydar2=[
    ["Mithril Pickaxe", (1)],
    ["Beacon Tier II", (lbin("beacon II") - (lbin("beacon I") + bz("refined mithril") * 5)) / (20 * quick_forge_bonus)],
    ["Titanium Talisman", 1],
    ["Diamonite", (lbin("diamonite") - (bz("refined diamond") * 3)) / (6 * quick_forge_bonus)],
    ["Power Crystal", (bzsell("power crystal") - (bzsell("starfall") * 256)) / (2 * quick_forge_bonus)],
    ["Refined Mithril Pickaxe", (lbin("refined mithril pickaxe") - (bz("refined mithril") * 3 + lbin("bejeweled handle") * 2 + bz("refined diamond") + bz("enchanted gold") * 30)) / (22 * quick_forge_bonus)],
    ["Mithril Drill SX-R226", (lbin("Mithril Drill SX-R226") - (lbin("drill engine") + bz("refined mithril") * 3 + lbin("fuel tank"))) / (4 * quick_forge_bonus)],
    ["Mithril-Infused Fuel Tank", (lbin("mithril-infused fuel tank") - (lbin("mithril plate") * 3 + lbin("fuel tank") * 5)) / (10 * quick_forge_bonus)],
    ["Mithril-Plated Drill Engine", (lbin("mithril-plated drill engine") -(lbin("drill engine") * 2 + lbin("mithril plate") * 3)) / (15 * quick_forge_bonus)],
    ["Beacon III", (lbin("beacon III") - (lbin("beacon II") + bz("refined mithril") * 10)) / (30 * quick_forge_bonus)],
    ["Titanium Ring", (lbin("titanium ring") - (bz("refined titanium") * 8)) / (34 * quick_forge_bonus)],
    ["Pure Mithril", (lbin("pure mithril") - bz("refined mithril")) / (12 * quick_forge_bonus)],
    ["Rock Gemstone", (lbin("rock gemstone") - (bz("enchanted cobblestone") * 128 + bz("treasurite") * 64)) / (22 * quick_forge_bonus)],
    ["Petrified Starfall", (lbin("petrified starfall") - bz("starfall") * 512) / (14 * quick_forge_bonus)],
    ["Pesto goblin omelette", (lbin("Pesto goblin omelette") - (bz("goblin egg green") * 99 + bz("fine jade"))) / (20 * quick_forge_bonus)],
    ["Ammonite Pet", (lbin("ammonite") - (lbin("helix") + 300000)) / (288 * quick_forge_bonus)],
    ["Ruby Drill TX-15", (lbin("Ruby Drill TX-15") - (lbin("drill engine") + lbin("fuel tank") + bz("fine ruby") *6)) / (1 * quick_forge_bonus)],
    ["Mithril Drill SX-R326", (1)],
    ["Titanium-Plated Drill Engine", (lbin("Titanium-Plated Drill Engine") - (lbin("drill engine") * 10 + lbin("plasma") * 5 + lbin("mithril plate") * 4 + bz("refined titanium") * 5)) / (30 * quick_forge_bonus)],
    ["Goblin Omelette", (lbin("Goblin Omelette") - bz("goblin egg") * 99) / (18 * quick_forge_bonus)],
    ["Beacon IV", (lbin("beacon IV") - (lbin("beacon III") + bz("refined mithril") * 20 + lbin("plasma"))) / (40 * quick_forge_bonus)],
    ["Titanium Artifact", (lbin("Titanium Artifact") - (bz("refined titanium") * 12 + lbin("titanium ring"))) / (36 * quick_forge_bonus)],
    ["Hot Stuff", (lbin("Hot Stuff") - (bz("hard stone") * 128 + bz("rough amber"))) / (24 *  quick_forge_bonus)],
    ["Sunny Side Goblin Omelette", (lbin("Sunny Side Goblin Omelette") - (bz("goblin egg yellow") * 99 + bz("fine topaz"))) / (20 * quick_forge_bonus)],
    ["Gemstone Drill LT-522", (1)],
    ["Titanium Drill DR-X355", (lbin("DR-X355")-(lbin("drill engine")+lbin("fuel tank")+lbin("golden plate")*6+bz("refined titanium")*10+bz("refined mithril")*10))/(64*quick_forge_bonus)],
    ["Titanium Drill DR-X455", (1)],
    ["Titanium Drill DR-x555", (1)],
    ["Titanium-Infused Fuel Tank", (lbin("Titanium-Infused Fuel Tank") - (bz("refined titanium") * 10 + bz("refined diamond") * 10 + bz("refined mithril") * 10 + lbin("fuel tank") * 10)) / (25 * quick_forge_bonus)],
    ["Beacon V", (lbin("Beacon V") - (lbin("Beacon IV") + bz("refined mithril") * 40 + lbin("plasma") * 5)) / (26 * quick_forge_bonus)],
    ["Titanium Relic", (lbin("titanium relic") - (bz("refined titanium") * 20 + lbin("titanium artifact"))) / (72 * quick_forge_bonus)],
    ["Spicy Goblin Omelette", (lbin("spicy goblin omelette") - (bz("goblin egg red") * 99 + bz("flawless ruby"))) / (20 * quick_forge_bonus)],
    ["Gemstone Chamber", (lbin("gemstone chamber") - (bz("worm membrane") * 100 + lbin("gemstone mixture") + 25000)) / (4 * quick_forge_bonus)],
    ["Topaz Drill KGR-12", (1)],
    ["Ruby-polished Drill Engine", (lbin("ruby-polished drill engine") - (lbin("mithril-plated drill engine") + lbin("superlite motor") * 10 + bz("fine ruby") * 10)) / (20 * quick_forge_bonus)],
    ["Gemstone Fuel Tank", (lbin("gemstone fuel tank") - (lbin("titanium-infused fuel tank") + lbin("control switch") * 30 + lbin("gemstone mixture") * 10)) / (30 * quick_forge_bonus)],
    ["Blue Cheese Goblin Omelette", (lbin("Blue Cheese Goblin Omelette") - (bz("perfect sapphire") + bz("goblin egg blue") * 99)) / (20 * quick_forge_bonus)],
    ["Titanium Drill DR-X655", (1)],
    ["Jasper Drill", (1)],
    ["Sapphire-polished Drill Engine", (lbin("Sapphire-polished Drill Engine") - (lbin("titanium-plated drill engine") + lbin("electron transmitter") * 25 + lbin("ftx 3070") * 25 + bz("fine sapphire") * 20)) / (20 * quick_forge_bonus)],
    ["Amber Material", (lbin("amber material") - (bz("fine amber") * 12 + lbin("golden plate"))) / (7 * quick_forge_bonus)],
    ["Helmet Of Divan", (lbin("helmet of divan") - (lbin("divan fragment") * 5 + lbin("gemstone mixture") * 10 + bz("flawless ruby"))) / (23 * quick_forge_bonus)],
    ["Chestplate Of Divan", (lbin("chestplate of divan") - (lbin("divan fragment") * 8 + lbin("gemstone mixture") * 10 + bz("flawless ruby"))) / (23 * quick_forge_bonus)],
    ["Leggings Of Divan", (lbin("leggings of divan") - (lbin("divan fragment") * 7 + lbin("gemstone mixture") * 10 + bz("flawless ruby"))) / (23 * quick_forge_bonus)],
    ["Boots Of Divan", (lbin("boots of divan") - (lbin("divan fragment") * 4 + lbin("gemstone mixture") * 10 + bz("flawless ruby"))) / (23 * quick_forge_bonus)],
    ["Amber-polished Drill Engine", (lbin("amber-polished drill engine") - (lbin("ruby-polished drill engine") + lbin("sapphire-polished drill engine") + bz("flawless amber") + lbin("robotron reflector") * 50)) / (50 * quick_forge_bonus)],
    ["Perfectly-Cut Fuel Tank", (lbin("Perfectly-cut fuel tank") - (lbin("gemstone fuel tank") + lbin("gemstone mixture") * 25 + lbin("synthetic heart") * 70)) / (50 * quick_forge_bonus)],
    ["Travel Scroll to the Dwarven Forge", (lbin("Travel Scroll to the Dwarven Forge") - (bz("mithril")*48 + bz("titanium")*48 + bz("enchanted ender pearl")*16+25000)) / (5 * quick_forge_bonus)],
    ["Travel Scroll to the Crystal Hollows", (lbin("Travel Scroll to the Crystal Hollows") - (bz("flawed ruby")*48 + bz("fine ruby")*48 + bz("enchanted ender pearl")*16+50000)) / (10 * quick_forge_bonus)],
    ["Hydar's Drill", (lbin("divan's drill") - (lbin("divan's alloy") + lbin("titanium drill dr-x655") + 50000000)) / (60 * quick_forge_bonus)]
 ]



hydar1=[[a,round(b)] for [a,b] in hydar1]
hydar2=[[a,round(b)] for [a,b] in hydar2]

hydarA=sorted(hydar1,key=lambda x: x[1])[::-1]
hydarB=sorted(hydar2,key=lambda x: x[1])[::-1]

print("REFINING:")
for [a,b] in hydarA:print(""+a+" at "+str(b))
print("\nCASTING:")
for [a,b] in hydarB:print(""+a+" at "+str(b))


