# PO_Proj_1
 
Znane błędy i niedokończone rzeczy:
 - dominanta z genotypów nie jest pokazywana w statystykach
 - mapa ma zadany rozmiar - nie skaluje się
 - przyciskami 'Stop', 'Resume' możemy pauzować/wznawiać symulację, ale jedynie całej mapy.
   W planach było rozgraniczenie tej funkcji dla obu map.
 - pole 'Refresh Time' nie jest używane. Miało służyć, jako
   czas pomiędzy odświeżeniami obrazu, ale ostatecznie to również jest ustaloną wartością
 - zapisywanie do pliku CSV jest bez uśrednionych wartości na końcu
 - dla początkowych zwierząt/trawy pojawia się białe tło - włożenie obiektu Stack Pane pod spód
   rozwiązywało by sprawę, ale nie zdążyłem tego naprawić
 - w dżungli trawa nie pojawia się na 2 bokach - błąd zapewne leży w indeksowaniu/granicznych wartościach
   przy dodawaniu trawy. Prosty błąd, ale nie zdążyłem go naprawić.
 - Pozostały gdzieniegdzie nieusunięte zakomentowane linijki kodu/komentarze TODO. Starałem się zdążyć przed
   północą z przesłaiem na GitHuba, stąd mogłem coś pominąć.
 - Taką pozostałością jest również np. funkcja 'positionChanged' w interfejsie 'IMapElement' i w konsekwencji w klasie abstrakcyjnej
   'AbstractMapElement', której w początkowym zamyśle było wykonywanie określonych operacji w momencie, gdy zarejestrowaliśmy poruszenie
   zwierzęcia, jednak została ona zaimplementowana na poziomie mapy.
   
   
Opis ciekawszych funkcjonalności:
 - przy aktualizowaniu interfejsu graficznego używanych jest kilka wątków: na głównym wątku działa sam interfejs,
   na kolejnym SimulationEngine (czyli klasa odpowiadająca za całą symulację), drawThread - wątek nasłuchujący
   sygnałów od SimulationEngine oraz aktualizujący interfejs i saveStatisticsThread, który również nasłuchuje
   sygnałów od SimulationEngine i zapisuje obecne statystyki do pliku CSV.

 - Klikanie na zwierzęta może odbywać się tylko podczas zatrzymania symulacji. Przeciwdziała to różnym sytuacjom,
   w których podczas wykonywania istotnych operacji, zmienne zostają zresetowane i ostatecznie otrzymujemy np.
   NullPointerException.
   
 - Zwierzęta zmieniają kolor w zależności od posiadanej energii. Dla energii bliższej dwukrotnie większej niż zadana energia
   początkowa, ich kolor przybiera barwę zieloną, dla energii bliższej zeru - czerwoną. (Dla wyszych wartości niż 2 x energia
   początkowa ich kolor pozostaje taki sam, czyli zielony)
   
 - Dla obserwowania zmian dla zaznaczonego obecnie zwierzęcia nie potrzebne było używanie Obserwatora. Zamiast tego, zaznaczonemu
   zwierzęciu zostaje zmieniona zmienna boolean, dzięki której wiemy, że jest ono obserwowane. Przy rozmnażaniu się obserwowanego
   zwierzęcia wiemy więc, że należy zaktualizować dane oraz jego dziecko również staje się obiektem obserwacji. Referencje do
   obserwowanych zwierząt trzymane są w pomocniczej liście, dzięki czemu przy zmianie zaznaczenia możemy łatwo dla każdego
   odznaczyć jego obserwowanie i wyczyścić listę.
   
 - Miejsca do rozmnażania oraz jedzenia typowane są na etapie poruszania się zwierząt. Do pomocniczych Setów zawierających obiekty
   Vector2d zapisywane są miejsca, gdzie zostanie zjedzona trawa (zwierzę weszło na pole z trawą) / gdzie może dojść do rozmnażania
   (jest tam inne zwierzę). Oczywiście, o ile trawa nie ma opcji poruszyć się z danego miejsca, o tyle inne zwierze może to zrobić,
   więc na etapie jedzenia zostaje dodatkowo sprawdzony warunek, czy nadal znajdują się tam >= 2 zwierzęta.
