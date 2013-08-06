vigilance
=========

This is an attempt to implement the Psychomotor Vigilance Task
(http://en.wikipedia.org/wiki/Psychomotor_vigilance_task)

It's written using Clojure and Clojurescript, because I wanted an
excuse to try Clojurescript =)

Set up and start the server like this:

    $ lein deps
    $ lein cljsbuild once
    $ lein ring server-headless 3000

Now, point your web browser at `http://localhost:3000`, and click whenever the
screen turns red.
