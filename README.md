# Zuständigkeiten:

## Louis Rau:
Ich, Louis Rau, habe mich um die Erstellung und Implementierung der Buchungsservices für sowohl Flüge, als auch Hotels gekümmert und alle nötigen schriftlichen Dokumente, einschließlich Beschreibung der Architektur und des Testkonzepts, angefertigt.

## Konrad Lorenz:
Initialisierung (angelehnt an Kims Vorarbeit) und Planung des Projekts . Client implementierung, TestingFunction(Main), CLI(Angelehnt an Tyler), MessageSenderService, Travel-/Messagebroker Init.
Anbindung Properties. BookingRequestParser. BookingSystemInterface geschrieben. Überarbeitung TravelBroker. Logging.
(Initialisierung: Ich habe die grundlegende Struktur aufgebaut. Jedoch wurde diese erweitert und angepasst)
## Uiyoung Kim:
Ich, Uiyoung Kim, habe mich um die Struktur, Planung und Initialisierung des Projekts gekümmert, die Grundstruktur der Server und Klassen erstellt, die Logik für Buchung und Messagebroker hinzugefügt und im Travelbroker Fehlerfälle bearbeitet. Zudem habe ich bei der Dokumentation geholfen.

## David Alexander Eugen Wolf:


## Sean Tyler Straub:
Ich, Sean Tyler Straub, habe mich um die Planung des Projekts gekümmert, den Travelbroker entwickelt, Großteile des Messagebrokers umgesetzt, bei den Buchungsservices geholfen, die Überarbeitung des Interfaces durchgeführt, beim BookingRequest unterstützt, die Bearbeitung der Main übernommen und die CLI erstellt. Dazu habe ich das Testkonzept überarbeitet.

# Genereller Ablauf der Programmierung
Durch das Zusammenarbeiten in Intellijs "Code Together" ist in den Git Commits nicht ersichtlich wer was gemacht hat. Deswegen gibt es hier eine kurze Übersicht getrennt nach Systemen. 

(Bei mehreren Namen in einer Klammer hat der Erste in der Klammer einen größeren Anteil übernommen)
## BookingSysteme
1. Struktur und erste Tests (Kim)
2. Erste Logik-Implementierung (Kim)
3. Übernahme des Konzepts in neues Projekt - keine Funktionalität (Tyler und Konrad)
4. Implementierung der Kernfunktionalität und Kommunikation (Louis und Tyler)
5. Fehlerzustände und Idempotenz (David, Tyler und Louis)
## Messagebroker
1. Struktur und Konzept (Kim)
2. Überarbeitung des Konzepts (Konrad)
3. Verteilung der Nachrichten Implentierung (Konrad)
4. Auslagerung in MessageSenderService (Konrad und Tyler)
5. Implementieren der Logik für das Schicken der Nachrichten (Tyler)
6. Clientcommunication (David)
7. Testing (Tyler)
## Travelbroker
1. Struktur und Konzept (Kim)
2. Überarbeitung Struktur und Konzept (Konrad)
3. Weitere Überarbeitung (Tyler)
4. Implementierung von Logik und Funktionalität (Tyler und Kim)
5. Überarbeitung der Struktur - Logik wurde leicht angepasst für Fehlerverarbeitung (Konrad und David)
6. Testing (David und Konrad)
## Andere
- Architekture (Louis, Kim, Tyler und David)
- Testkonzept (Louis, Kim und Tyler)
- Ehrenwörtliche Erklärung (Louis)
- CLI (Konrad und Tyler)
- Main (Tyler und Konrad)
- Properties (Konrad)
- Booking, BookingRequest, BookingRequestParser (Konrad)
