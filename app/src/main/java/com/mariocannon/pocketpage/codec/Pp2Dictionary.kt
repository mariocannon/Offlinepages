package com.mariocannon.pocketpage.codec

/**
 * The PP2 shared dictionary: text that small pages nearly always contain (HTML tags, CSS, common
 * words). The compressor points back into it instead of storing those bytes on the tag.
 *
 * FROZEN. Every PP2 code and tag ever made depends on these exact bytes. Changing one character
 * breaks them all, so a better dictionary needs a new prefix (PP3). The web app has the same
 * text in web/codec.js; `PageCodecTest.dictionaryIsFrozen` guards both (14364 B, CRC32 94c62460).
 * UTF-8 (it has macrons and a few symbols). The most common strings sit at the end, where
 * they're cheapest to point at. Deflate can only reach back 32 KB, so keep it well under that.
 */
object Pp2Dictionary {
    private const val TEXT =
        "radio radios antenna antennas battery batteries charger charging charge solar panel power " +
        "signal reception range network networks mesh node nodes relay repeater message messages me" +
        "ssaging channel channels encrypted encryption GPS location locations map maps Bluetooth Wi" +
        "Fi wifi internet online offline phone phones smartphone tablet laptop computer app apps An" +
        "droid iOS iPhone Samsung Google Apple hardware software board boards firmware update updat" +
        "es install download flash USB Chrome browser device devices sensor sensors data temperatur" +
        "e humidity setting settings default region frequency MHz GHz kilometres metres km hop hops" +
        " router routers client server tower cell coverage interference preset presets speed faster" +
        " slower longer shorter QR code codes scan scanner NFC tag tags sticker stickers password u" +
        "sername login account screen camera video photo photos audio speaker microphone track trac" +
        "ks trail trails walkway lookout summit ridge valley bush forest native trees tree birds bi" +
        "rd tui kereru fantail ruru weka kiwi fern ferns pohutukawa kauri beach beaches coast coast" +
        "al harbour estuary river rivers stream waterfall lake island islands sea ocean surf waves " +
        "tide tides swim swimming kayak kayaks paddle boat boats fishing hunting tramping camping c" +
        "ampsite hut huts picnic dog dogs lead leash rubbish toilets parking carpark track closed w" +
        "eather rain wind sunny forecast summer winter autumn spring sunrise sunset skate skatepark" +
        " bike bikes cycling running walking hiking golf tennis netball rugby football cricket pick" +
        "leball shop store cafe café restaurant bar takeaway menu coffee flat white long black tea " +
        "juice smoothie breakfast brunch lunch dinner dessert cake pie pies scones toastie burger f" +
        "ries pizza vegan vegetarian gluten dairy nut allergies order orders delivery pickup collec" +
        "t booking bookings book online appointment appointments quote quotes invoice receipt payme" +
        "nt pay card cash EFTPOS Afterpay price prices pricing cost costs per hour day week month y" +
        "ear deposit refund refunds return returns hire rental rentals rent available availability " +
        "stock sale sales discount offer offers deal deals special specials voucher gift membership" +
        " member members points reward rewards prize draw entry entries enter win winner terms cond" +
        "itions apply safety safe unsafe warning danger caution hazard hazards PPE helmet hard hat " +
        "gloves glasses boots hi-vis harness ladder scaffold scaffolding platform bay bays lift lif" +
        "ts load duty inspection inspected inspector tagged register sign signed site manager contr" +
        "actor builder electrician plumber builder licensed certified compliance standard standards" +
        " report damage repair repairs maintenance service serviced check checked checklist fire ex" +
        "tinguisher exit first aid emergency evacuation assembly point alarm alarms access door doo" +
        "rs gate gates key keys lock locked unlocked code entry visitor visitors reception building" +
        " floor level unit units office event events market markets festival concert show workshop " +
        "class classes lesson lessons course term holidays school schools kindergarten kids childre" +
        "n child family families parents parent baby toddler teens adults seniors community club cl" +
        "ubs group groups team teams volunteer volunteers fundraiser donation donations free entry " +
        "tickets ticket raffle music band live food stalls crafts market night morning tea church h" +
        "all library museum park playground pool beach reserve council local locals neighbours neig" +
        "hbourhood newsletter news update updates once upon a time there was a little big small tin" +
        "y huge happy sad brave scared kind friend friends together home forest moon stars sun sky " +
        "cloud clouds rain snow wind sea fish bird birds owl snail rabbit bear fox dog cat mouse du" +
        "ck frog turtle dragon princess king queen castle magic adventure journey dream dreams slee" +
        "p asleep night nap whispered shouted laughed smiled cried sang flew ran jumped looked walk" +
        "ed went came said asked replied the end  New Zealand Aotearoa Auckland Tamaki Makaurau Wel" +
        "lington Christchurch Hamilton Tauranga Dunedin Napier Nelson Queenstown Rotorua Whangarei " +
        "Northland Waikato Bay of Plenty Canterbury Otago Southland Coromandel North Shore Albany T" +
        "akapuna Devonport Henderson Manukau Hibiscus Coast Hibiscus Coast Highway Orewa Ōrewa Whan" +
        "gaparaoa Whangaparāoa Silverdale Millwater Red Beach Stanmore Bay Gulf Harbour Army Bay Ar" +
        "kles Bay Manly Waiwera Puhoi Pūhoi Wenderholm Warkworth Matakana Snells Beach Kaipara Rodn" +
        "ey Helensville Dairy Flat Wainui Waitemata Gulf Shakespear Tiritiri Matangi Hauraki  kia o" +
        "ra Kia ora mōrena tēnā koe tēnā koutou haere mai nau mai ngā mihi mā te wā ka kite anō whā" +
        "nau whanau kai koha mahi mana marae hapū iwi tamariki rangatahi kaumātua kaitiaki manaakit" +
        "anga kaitiakitanga whenua moana awa maunga ngahere tangata Māori Pākehā tāne wāhine aroha " +
        "pai ka pai mīharo tūī Tūī kererū pīwakawaka ruru pōhutukawa kōwhai harakeke Matariki waka " +
        "pā haka whare kura wharekura  NZ NZD GST incl excl Ltd Limited NZTA AT HOP Countdown Woolw" +
        "orths New World Pak'nSave Bunnings Mitre 10 Trade Me TradeMe Facebook Instagram TikTok You" +
        "Tube Google Maps Waze 0800 021 022 027 029 09 +64  able above accept access account across" +
        " act action active actually add added address adult advice afraid age agree ahead air allo" +
        "w almost alone along already also although always amazing amount another answer anyone any" +
        "thing anywhere apart area around arrive art article ask asked attention aunt available ave" +
        "rage avoid away awesome baby back bad bag ball bank base basic bed been began begin behind" +
        " believe below beside better between beyond bill bit black blue body both bottle bottom bo" +
        "x boy brand break bright bring brother brown build built busy buy cake call called came ca" +
        "nnot car care careful carry case catch cause centre certain chance change changes charge c" +
        "heap check child choice choose city clean clear close closed closes clothes colour come co" +
        "mes comfortable common company complete contact continue control cook cool copy corner cor" +
        "rect cost could count country couple course cover create cross cup current cut daily dark " +
        "date daughter dear decide deep definitely deliver design detail details different difficul" +
        "t direct direction dirty discover distance doing done double down draw drink drive drop dr" +
        "y during each early easily east easy edge eight either else email end enjoy enough ensure " +
        "enter equipment even evening ever every everyone everything exactly example except excited" +
        " expect experience explore extra eye face fact fair fall far farm fast father favourite fe" +
        "el feet felt few field fill final finally fine finish first five fix floor follow followin" +
        "g foot force form forward found four friendly front full fun further future game garden ga" +
        "ve general gentle get gets getting girl give given glad goes going gold gone good great gr" +
        "een ground grow guide guests half hand hands hang happen hard head health hear heard heart" +
        " heavy held help helps her here high him his hold hole hope hot hour hours house however h" +
        "undred idea important include included including inside instead interest join journey jump" +
        " just keep kept kind kitchen knew know known land large last late later learn least leave " +
        "left less let letter life light like likely line list listen little live lived living long" +
        " look looking lose lost lot loud love lovely low made main make makes making man many mark" +
        " matter may maybe mean meet meeting middle might mind minute minutes miss modern moment mo" +
        "ney month months more most mother move much must name near nearly need needs never next ni" +
        "ce nine none north note nothing notice number off office often old once one only onto open" +
        " order other others outside over own paper part party pass past path people perfect person" +
        " pick picture piece place places plan plant play please plenty point possible post pretty " +
        "problem problems program protect provide pull push put question questions quick quickly qu" +
        "iet quite rather reach read ready real really reason red remember remove rest right road r" +
        "oom round rule rules run safe same save saw say says second section see seen send sense se" +
        "t seven several shall share short should show side simple since single sister sit six size" +
        " slowly smell soft some someone something sometimes son soon sorry sound south space speak" +
        " special spend spot stand start started stay step steps still stop story straight street s" +
        "trong study stuff such sure surprise table take taken talk teach tell ten test than thank " +
        "thanks that's the then thing things think third though thought three through throw today t" +
        "old too took top total touch toward town travel tried true try turn twice two type under u" +
        "nderstand until upon use useful usually various very view visit wait walk wall want warm w" +
        "ash watch water way ways wear welcome well went west what's whatever wheel while white who" +
        "le why wide wife will window wish within without woman wonderful word words work works wor" +
        "ld worry write wrong yard yes yet you'll you're young yourself  Get Find Call Book Visit J" +
        "oin Open Read Learn Tap Scan Please Thanks Thank you Welcome Hello Hi We Our You Your It T" +
        "his The A An If For With Free New Opening hours Contact us About us Find out more Get in t" +
        "ouch Read more Learn more Book now Call now Shop now Sign up Subscribe Order online Tap he" +
        "re Scan here  for more information if you have any questions please contact us we are open" +
        " every day we look forward to seeing you thank you for your support don't forget to bring " +
        "make sure you you can also you will need you need to you don't need it is a great way to t" +
        "here is no need to as soon as possible at any time all year round free of charge for the w" +
        "hole family just a short walk from in the heart of one of the best is one of the only a fe" +
        "w minutes from on the day subject to availability while stocks last terms and conditions a" +
        "pply  Mon Tue Wed Thu Fri Sat Sun Mon–Fri Sat–Sun Monday to Friday weekdays weekends publi" +
        "c holidays 6am 7am 8am 9am 10am 11am 12pm 1pm 2pm 3pm 4pm 5pm 6pm 7pm 8pm 9pm 10pm 9:30am " +
        "10:30am 4:30pm 5:30pm 6:30pm midday midnight 7 days 24/7 9am–5pm 8am–4pm 10am–2pm  \$1 \$2 \$" +
        "3 \$4 \$5 \$6 \$7 \$8 \$9 \$10 \$12 \$15 \$20 \$25 \$30 \$40 \$50 \$100 .50 .00 /hr /day /week each pp fr" +
        "om just only 10% 20% 50% off half price free  1st 2nd 3rd 4th 5th No. Rd Road St Street Av" +
        "e Avenue Dr Drive Pl Place Cres Crescent Hwy Highway Lane Way Tce Terrace Unit Level Ph Te" +
        "l Email Web www. .co.nz .nz .com  – — · • → ← ✓ ✔ ✗ ★ ° ½ © ® ’ ‘ “ ” … & &amp; &nbsp;  in" +
        " the of the to the on the at the for the from the with the by the and the is the it is thi" +
        "s is that is there are there is you can you will you need we have we are we will we can I " +
        "will I can will be can be has been have been is a it's a a few a lot of a little all the o" +
        "ne of some of most of out of up to as well as well so you can so we can how to what to whe" +
        "re to when to want to need to have to going to able to used to make sure find out come and" +
        " get your our team our friendly team the whole family each other  a about after again all " +
        "also am an and any are as at back be because been before being best but by can can't come " +
        "could day did do does don't down each even every first for from get go going good got grea" +
        "t had has have he her here him his how I I'm if in into is it it's its just know last like" +
        " little look make many may me more most much must my new next no not now of off on one onl" +
        "y or other our out over own people really right said see she should so some still such tak" +
        "e than that the their them then there these they thing think this those time to too two up" +
        " us use very want was way we well were what when where which while who why will with would" +
        " year years yes you your #000#111#222#333#444#555#666#777#888#999#aaa#bbb#ccc#ddd#eee#fff#" +
        "f6f1e7#2f4a3alinear-gradient(radial-gradient(rgba(0,0,0,.box-shadow:0 2px 8px transition:t" +
        "ransform:translate(rotate(animation:@keyframes @media(min-width:600px){grid-template-colum" +
        "ns:1fr 1fr;gap:flex-direction:column;justify-content:space-between;align-items:center;posi" +
        "tion:relative;absolute;sticky;top:0;left:0;right:0;overflow:hidden;white-space:nowrap;opac" +
        "ity:cursor:pointer;letter-spacing:.1em;text-transform:uppercase;font-style:italic;font-wei" +
        "ght:600;font-weight:700;font-weight:bold;line-height:1.5;list-style:none;border-top:1px so" +
        "lid #border-bottom:border-left:border:1px solid #border-radius:12px;border-radius:50%;box-" +
        "sizing:border-box;width:100%;height:100%;max-width:100%;min-height:100vh;text-decoration:n" +
        "one;text-align:left;text-align:center;display:inline-block;display:flex;display:grid;displ" +
        "ay:block;margin-top:1em;margin-bottom:margin:0 auto;margin:0;padding:0 padding:1em;padding" +
        ":12px 16px;font-size:1.2em;font-size:14px;font-family:font:17px/1.5 Georgia,serif;font:17p" +
        "x/1.6 system-ui,sans-serif;background:linear-gradient(background:#fff;color:#fff;color:#}h" +
        "1{}h2{}h3{}p{}a{}b{}i{}ul{}ol{}li{}small{}header{}footer{}main{}section{}nav{}div{}span{}i" +
        "mg{}svg{}code{}.c{}.b{}*{box-sizing:border-box}html{}body{max-width:36em;margin:auto;paddi" +
        "ng:5vw;<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"height=\"fill=\"" +
        "none\" stroke=\"stroke-width=\"<path d=\"M<circle cx=\"cy=\"r=\"<rect x=\"y=\"<text x=\"</svg><scrip" +
        "t>document.getElementById(querySelector(addEventListener('click',function(){onclick=textCo" +
        "ntent=innerHTML=style.classList.toggle(</script><table><tr><th></th><td></td></tr></table>" +
        "<img src=\"data:image/svg+xml,alt=\"<button><input type=\"<label><form><br><hr><em></em><stro" +
        "ng></strong><i></i><code></code><small></small><span></span><div class=\"</div><div class=<" +
        "div id=<section></section><header></header><footer></footer><main></main><nav></nav> targe" +
        "t=_blank rel=noopener href=\"tel:+64href=\"mailto:href=\"sms:href=\"geo:href=\"https://maps.goo" +
        "gle.com/?q=href=\"https://www.facebook.com/href=\"https://www.instagram.com/.co.nz/\">.com/\">" +
        ".org/\">.nz\"></a><a href=https://www.<a href=\"https://<dl><dt></dt><dd></dd></dl><ol><li></" +
        "ol><ul><li><li><b></b></ul></li><li></p><p><b></b><p></h3><h3></h2><h2></h1><h1><meta name" +
        "=\"pocketpage-book\" content=\"<html lang=\"en\"><head></head><body></body></html><title></titl" +
        "e><style></style><body class=pp-plain><body class=pp-book><body class=pp-dark><!DOCTYPE ht" +
        "ml><!doctype html><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width," +
        "initial-scale=1\"><meta charset=utf-8><meta name=viewport content=\"width=device-width\"><met" +
        "a charset=utf-8><meta name=viewport content=width=device-width><title>"

    val BYTES: ByteArray = TEXT.toByteArray(Charsets.UTF_8)
}
