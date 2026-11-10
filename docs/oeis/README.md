# A031173, A031174 and A031175, carried on

The OEIS sequences [A031173](https://oeis.org/A031173), [A031174](https://oeis.org/A031174) and
[A031175](https://oeis.org/A031175) give the longest, middle and shortest edges of the primitive
Euler bricks in order of longest edge. Their b-files list 3,556 bricks, with longest edge up to
499,907,100. These three files carry them on to 7,004 bricks, every one with longest edge up to
3,000,000,000.

- Rows 1 to 3,556 are the published rows, exactly, ties in their published order. The hunt's
  file was first checked to hold exactly those bricks over their range.
- Rows 3,557 to 7,004 come from the hunt of every primitive brick with smallest edge up to
  3·10⁹, which `verify` checked again with exact arithmetic. A brick's smallest edge is no
  longer than its longest, so that hunt holds every brick with longest edge up to 3·10⁹. Ties are
  in order of the middle edge.

Made with

```sh
java -Xmx2g -jar perfectcuboid.jar bfiles hunt-primitive.txt docs/oeis --published DIR
```

where DIR holds the published b031173.txt, b031174.txt and b031175.txt. They are written in the
OEIS's b-file format, ready to offer; whether to offer them, and under whose name, is for a
person to decide.
