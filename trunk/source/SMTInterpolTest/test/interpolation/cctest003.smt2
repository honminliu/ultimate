(set-option :produce-interpolants true)
(set-option :interpolant-check-mode true)
(set-logic QF_UF)
(declare-sort U 0)
(declare-fun a () U)
(declare-fun b () U)
(declare-fun g (U) U)
(assert (! (= a b) :named IP1))
(assert (! (not (= (g a) (g b))) :named IP2))
(check-sat)
(get-interpolants IP1 IP2)
(get-interpolants IP2 IP1)
(exit)
