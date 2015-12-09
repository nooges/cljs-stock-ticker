Stock Ticker
============

Stock ticker display written in Clojurescript

This is my first attempt ever at writing anything in Clojure/Clojurescript, so please excuse any weirdness or inefficiencies in my code. The last time I did something with Lisp was about 15 years ago while learning Scheme.

Goals
-----
- Pull down ticker data from Google Finance or CNBC
    - Convert data to common format
- Display ticker information
    - Symbol
    - Name
    - Current price
    - Price change since open
    - Percentage change since open
    - Last time
    - Extended hours
        - Toggle between change since open/close
- Modify title, do scrolling
- Configuration settings
    - Allow tickers to be added/removed
    - Polling interval