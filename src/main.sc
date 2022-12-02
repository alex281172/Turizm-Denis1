require: slotfilling/slotFilling.sc
  module = sys.zb-common

require: localPatterns.sc

require: dicts/cities.csv
    name = cities
    var = cities

require: dicts/countries.csv
    name = countries
    var = countries

require: dicts/names.csv
    name = names
    var = names
    
require: functions.js

init:
    bind("postProcess", function($context) {
        $context.session.lastState = $context.currentState;
    });
# нужен ?
init:
    $global.$ = {
        __noSuchProperty__: function(property) {
            return $jsapi.context()[property];
        }
    };


theme: /
    state: Start
        q!: $regex</start>
        q!: *start
        q!: $hi *
        script:
            $session = {};
        if: $client.name
            a: Здравствуйте, {{$client.name}}. Я - бот туристической компании «Just Tour». Я могу расказать Вам о погоде в мире, а также помочь оформить заказ на турпутёвку.
        else: 
            a: Здравствуйте, меня зовут {{ $injector.botName }}, я - бот туристической компании «Just Tour». Я могу расказать Вам о погоде в мире, а также помочь оформить заказ на турпутёвку.
        a: Что Вас интересует?
        buttons:
            "Путёвка"
            "Погода"
 
        state: GetTour
            q: Путёвка
            go!: /Trip/TripStartPoint
        state: GetWeather
            q: Погода
            go!: /Weather/WeatherQust
                
       

    state: CatchAll || noContext = true
        event!: noMatch
        a: Простите, мне не понятно. Попробуйте написать по другому.




#============================================= ПОГОДА =============================================#

theme: /Weather 
    
    state: WeatherQust 
        #вопрос из любого места о погоде без конкретики. Если до этого погоду уже спрашивали, то уточнит по месту. 
        q!: * (~погода) *
        if: $session.arrivalPointForCity
            a: Вы хотели бы узнать погоду в {{ $session.arrivalPointForCity }}?
        elseif: $session.arrivalPointForCountry
            a: Вы хотели бы узнать погоду в {{ $session.arrivalPointForCountry }}?
        else:
            a: В Каком Городе/Стране Вас интересует погода?
        
        state: WeatherYes
            q: * $comYes *
            if: $session.arrivalPointForCity
                go!: /Weather/WeatherOfCity
            elseif: $session.arrivalPointForCountry
                go!: /Weather/WeatherOfCountry
            
        state: WeatherNO
            q: * $comNo *
            script:
                delete $session.arrivalPointForCity;
                delete $session.arrivalPointForCountry;
                delete $session.arrivalCoordinates;
            go!: /Weather/WeatherQust
        
        state: WeatherCity
            q: * $City *
            go!: /Weather/WeatherInTheCity
        
        state: WeatherCountry
            q: * $Country *
            go!: /Weather/WeatherInTheCountry
    
    
    
    state: WeatherInTheCity
        #вопрос из любого места о погоде в конкретном городе
        q!: * [$Question] * $Weather * $City *
        q!: * [$Question] * $City * $Weather *
        q!: * а в $City *
        script:
            $session.arrivalPointForCity = $parseTree._City.name;
            $session.arrivalCoordinates = {
                lat: $parseTree._City.lat,
                lon: $parseTree._City.lon
            };
            $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
            $session.TempForQuest = $temp.Weather.temp;
        if: $temp.Weather
            a: В городе {{ $session.arrivalPointForCity }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
            go!: /Weather/AreYouSure

    
    state: WeatherInTheCountry 
        #вопрос из любого места о погоде в конкретной стране
        q!: * [$Question] * $Weather * $Country *
        q!: * [$Question] * $Country * $Weather *
        q!: * а в $Country *
        script:
            $session.arrivalPointForCountry = $parseTree._Country.namesc;
            $session.arrivalCoordinates = {
                lat: $parseTree._Country.lat,
                lon: $parseTree._Country.lon
            };
            $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
            $session.TempForQuest = $temp.Weather.temp;
        if: $temp.Weather
            a: В {{ $session.arrivalPointForCountry }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
            go!: /Weather/AreYouSure
    

    state: WeatherOfCity 
        #для варианта когда уже был известен город и сессионая переменная уже обозначена
        script:
            $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
            $session.TempForQuest = $temp.Weather.temp;
        if: $temp.Weather
            a: В городе {{ $session.arrivalPointForCity }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
            go!: /Weather/AreYouSure
        
            
    state: WeatherOfCountry 
        #для варианта когда уже была известна страна и сессионая переменная уже обозначена
        script:
            $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
            $session.TempForQuest = $temp.Weather.temp;
        if: $temp.Weather
            a: В {{ $session.arrivalPointForCountry }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
            go!: /Weather/AreYouSure
        
        
    state: AreYouSure
        #уточнение намерения клиента поехать при данной температуре
        script:
            if ($session.TempForQuest < 25 && $session.TempForQuest > 0) {
                $reactions.answer("Вы хотели бы запланировать поездку в страну с умеренным климатом?");
            }
            else if ($session.TempForQuest <= 0) {
                $reactions.answer("Вы хотели бы запланировать поездку в страну с холодным климатом?");
            }
            else if ($session.TempForQuest > 25 || $session.TempForQuest == 25) {
                $reactions.answer("Вы хотели бы запланировать поездку в страну с жарким климатом?");
            }

        state: YesSure
            q: * $comYes *
            if: $session.StartPoint
                a: Вы хотели бы начать оформление нового тура в данную страну или хотели бы продолжить оформление старой заявки? 
            else: 
                a: Вы хотели бы начать оформление нового тура в данную страну? 

            state: New
                q: * $comYes *
                q: верно
                q: * [это] (он/оно/то что нужно) *
                q: * (новый/новую/нового/сначала) *
                q: * (заного/заново) *
                script:
                    $session = {};
                    
                go!: /Trip/TripStartPoint
        
            state: Old
                q: * (~продолжить) *
                q: * (~старая/старую/~прошлая/прошлую/прошлый/старой/предыдущей/предыдушую) *
                go!: /Trip/TripStartPoint
        
        state: NoSure
            q: * $comNo *
            q: * (не верно/неверно) *
            q: * [это] не то [что я хотел] *
            a: Хотите узнать о погоде в другом Городе/Стране?

            state: YesNoSure
                q: * $comYes *
                q: верно
                q: * [это] (он/оно/то что нужно) *
                script:
                    delete $session.arrivalPointForCity;
                    delete $session.arrivalPointForCountry;
                    delete $session.arrivalCoordinates; 
                go!: /Weather/WeatherQust  
        
            state: NoNoSure
                q: * $comNo *
                q: * (не верно/неверно) *
                q: * [это] не то [что я хотел] *
                a: К сожалению, я больше ничем не могу Вам помочь.




#============================================= ОФОРМЛЕНИЕ ПУТЕВКИ =============================================#

theme:/Trip
    
    state: TripStartPoint
        q!: * (~Пут[её]вка/тур*) * 
        if: $session.StartPoint
            if: $session.departureCity
                go!: /Trip/TripStartPoint/DepartureCity 
            else:
                a: {{ $client.name }}, назовите, пожалуйста, город отправления. Если Вы ошибетесь в данных - не волнуйтесь, наш менеджер свяжется с Вами для уточнения данных.   
        else:
            a: {{ $client.name }}, назовите, пожалуйста, город отправления. Если Вы ошибетесь в данных - не волнуйтесь, наш менеджер свяжется с Вами для уточнения данных.
            script:
                $session.StartPoint = 1;
    
        state: DepartureCity
            q: * $City *
            if: $session.departureCity 
                if: $session.departureDate
                    go!: /Trip/TripStartPoint/DepartureCity/DepartureDate
                else:
                    a: Желаемая дата отправления?
            else:
                script:
                    $session.departureCity = $parseTree._City.name;
                if: $session.departureDate
                    go!: /Trip/TripStartPoint/DepartureCity/DepartureDate
                else:
                    a: Желаемая дата отправления?
            
            
            state: DepartureDate
                q: * (@duckling.date/@duckling.time) *
                if: $session.departureDate
                    if: $session.arrivalPointForCity  
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCity
                    elseif: $session.arrivalPointForCountry 
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCountry
                    else:
                        a: Куда бы вы хотели отправиться?
                else:
                    script:
                        $session.departureDate = $parseTree.value;
                    if: $session.arrivalPointForCity  
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCity
                    elseif: $session.arrivalPointForCountry 
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCountry
                    else:
                        a: Куда бы вы хотели отправиться?
                
                
                state: ArrivalCity
                    q: * $City *
                    if: $session.arrivalPointForCity  
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                    else:
                        script:
                            $session.arrivalPointForCity = $parseTree._City.name;
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                
                
                state: ArrivalCountry
                    q: * $Country *
                    if: $session.arrivalPointForCountry  
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                    else:
                        script:
                            $session.arrivalPointForCountry = $parseTree._Country.name;
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                
                
                state: CatchAll || noContext = true
                    event: noMatch
                    a: Простите, Но туда у нас нет туров. Выберете другую точку.
    
                    
    state: ReturnDate            
        q: * (@duckling.date/@duckling.time) *
        if: $session.returnDate
            if: $session.people
                go!: /Trip/ReturnDate/People
            else:
                a: Количество людей в поездке?
        else:
            script:
                $session.returnDate = $parseTree.value;
            if: $session.people
                go!: /Trip/ReturnDate/People
            else:
                a: Количество людей в поездке? 
        
        buttons:
            "уточню позже"    
            
        state: People
            q: *
            if: $session.people
                if: $session.children
                    go!: /Trip/ReturnDate/People/Children
                else:
                    a: Количество детей в поездке?
            else:
                script:
                    $session.people = $request.query;
                if: $session.children
                    go!: /Trip/ReturnDate/People/Children
                else:
                    a: Количество детей в поездке?
            
            buttons:
                "уточню позже"    
            
            state: Children
                q: * 
                if: $session.children
                    if: $session.budget
                        go!: /Trip/ReturnDate/People/Children/Budget
                    else:
                        a: Какой у Вас Бюджет?
                else:
                    script:
                        $session.children = $request.query;
                    if: $session.budget
                        go!: /Trip/ReturnDate/People/Children/Budget
                    else:
                        a: Какой у Вас Бюджет?
                 
                buttons:
                    "уточню позже"    
            
                state: Budget
                    q: *
                    if: $session.budget
                        if: $session.stars
                            go!: /Trip/ReturnDate/People/Children/Budget/Stars
                        else:
                            a: Сколько звезд у отеля ?
                    else:
                        script:
                            $session.budget = $request.query;
                        if: $session.stars
                            go!: /Trip/ReturnDate/People/Children/Budget/Stars
                        else:
                            a: Сколько звезд у отеля ?
            
                    buttons:
                        "уточню позже"    
            
                    state: Stars
                        q: *
                        if: $session.stars
                            if: $session.comments
                                go!: /Trip/ReturnDate/People/Children/Budget/Stars/Comments
                            else:
                                a: Комментарий для менеджера?
                        else:
                            script:
                                $session.stars = $request.query;
                            if: $session.comments
                                go!: /Trip/ReturnDate/People/Children/Budget/Stars/Comments
                            else:
                                a: Комментарий для менеджера?
                 
                        buttons:
                            "нет комментариев"    
            
                        state: Comments
                            q: *
                            if: $session.comments
                                go!: /Name/AskName
                            script:
                                $session.comments = $request.query;
                            if: $client.name    
                                go!: /Phone/Ask
                            else:
                                go!: /Name/AskName



#============================================= Запрос имени и телефона и окончание формирования путевки =============================================#
theme: /Name
    state: AskName || modal = true
        a: Как я могу к Вам обращаться ?

        state: GetName
            q: * $Name *
            script:
                $client.name = $parseTree._Name.name;
            go!: /Phone/Ask

        state: GetStrangeName
            q: * 
            a: Я знаю много имён, но такое вижу впервые. Вас точно так зовут?
            script:
                $session.probablyName = $request.query;
            
            buttons:
                "Да"
                "Нет"
            
            state: Yes
                q: (да/верно)
                script:
                    $client.name = $session.probablyName;
                    delete $session.probablyName;
                go!: /Phone/Ask

            state: No
                q: (нет/не [верно])
                go!: /Name/AskName
        
        state: NoName
            q: * (не скажу/не хочу/не буду/[а тебе] [не] зачем/еще чего/фиг/фигушки/нет/никак/[как] хочешь) *
            a: Ваше Имя нужно для оформления заявки,иначе я не смогу Вам помочь подобрать тур, могу только рассказать о погоде.    
            

    state: ConfirmName
        a: Ваше имя {{ $client.name }}, верно?
        script:
            $session.probablyName = $client.name;
        buttons:
            "Да"
            "Нет"

        state: Yes
            q: (да/верно)
            script:
                $client.name = $session.probablyName;
                delete $session.probablyName;
            go!: /Phone/Ask

        state: No
            q: (нет/не [верно])
            go!: /Name/AskName

theme: /Phone
    state: Ask 
        a: Для передачи заявки менеджеру, мне нужен Ваш номер телефона в формате 79000000000.

        state: Get
            q: $phone
            go!: /Phone/Confirm

        state: Wrong
            q: *
            a: О-оу. ошибка в формате набора номера. Проверьте.
            go!: /Phone/Ask


    state: Confirm
        script:
            $temp.phone = $parseTree._phone || $client.phone;

        a: Ваш номер {{ $temp.phone }}, верно?

        script:
            $session.probablyPhone = $temp.phone;

        buttons:
            "Да"
            "Нет"

        state: Yes
            q: (да/верно)
            script:
                $client.phone = $session.probablyPhone;
                delete $session.probablyPhone;
            go!: /SendMail/Mail
            

        state: No
            q: (нет/не [верно])
            go!: /Phone/Ask
            
            
#============================================= Отправка формы на почту менеджеру =============================================#            
        
theme: /SendMail
    state: Mail
        script:
            var ClientData = "";
            ClientData += " Имя: " + $.client.name + "\n";
            ClientData += " Город отправления: " + $.session.departureCity + "\n";
            ClientData += " Дата отправления: " + $.session.departureDate.year + "." + $.session.departureDate.month + "." + $.session.departureDate.day + "\n";
            if ($.session.arrivalPointForCity) {
                ClientData += " Город назначения: " + $.session.arrivalPointForCity + "\n";
            }
            if ($.session.arrivalPointForCountry) {
                ClientData += " Страна назначения: " + $.session.arrivalPointForCountry + "\n";
            }
            ClientData += " Дата возвращения: " + $.session.returnDate.year + "." + $.session.returnDate.month + "." + $.session.returnDate.day + "\n";
            ClientData += " Количество людей: " + $.session.people + "\n";
            ClientData += " Количество детей: " + $.session.children + "\n";
            ClientData += " Бюджет: " + $.session.budget + "\n";
            ClientData += " Звезд у отеля: " + $.session.stars + "\n";
            ClientData += " Комментарии для Менеджера: " + $.session.comments + "\n";
            ClientData += " Телефон: " + $client.phone + "\n";
            var varmail = $mail.sendMessage("d.smirnov44@gmail.com","Заявка от клиента "+ $.client.name, ClientData);
            if (varmail["status"] == "OK")
                {$reactions.answer("Спасибо за обращение, в ближайшее время с Вами свяжется менеджер Вашего заказа. До свидания.");
                $session = {};}
            else
                $reactions.answer("При отправке письма с запросом возникли неполадки. Для подбора тура обратитесь, пожалуйста, к менеджеру по тел. 8(812)000-00-00.", varmail["status"]);
            
 
