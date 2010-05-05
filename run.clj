;;; This is a script for running konersver.main/-main

(use '[konserver.main :only [-main]])
(apply -main *command-line-args*)

