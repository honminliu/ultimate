//#Safe
/*
 */
procedure main() {
  var i, j : int;
  var a : [int] int;

  a := ~const~Array~Int~Int(0);

  a[i] := 1;

  a[i] := 2;

  // a[j] in {0, 2}
  assert a[j] == 0 || a[j] == 2;
}

function { :builtin "const-Array-Int-Int" } ~const~Array~Int~Int(in : int) returns (out : [int] int);