patterns:
    $hi = (привет*/здравствуй*)
    $phone = $regexp<79\d{9}>
    $City = $entity<cities> || converter = function(parseTree) {var id = parseTree.cities[0].value; return cities[id].value;};
    $Country = $entity<countries> || converter = function(parseTree) {var id = parseTree.countries[0].value; return countries[id].value;};
    $Name = $entity<names> || converter = function(parseTree) {var id = parseTree.names[0].value; return names[id].value;};
    $Question = (какой|какая|что с|че с|че|что|как|подскажи)
    $Weather = (~погода|~прогноз)
    $comYes = (ага/так точно/конечно/естественно/а как же/да/да*/lf/rjytxyj/fuf/tcntccndtyyj/da/ok)
    $comNo = (нет/ytn/нету/нэту/неат/ниат/неа/ноуп/ноу/найн/нее/неее/нееее/неееее)