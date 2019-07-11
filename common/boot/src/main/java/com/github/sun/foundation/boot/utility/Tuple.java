package com.github.sun.foundation.boot.utility;

/**
 * @Author LinSH
 * @Date: 6:49 PM 2019-02-28
 */
public interface Tuple {
  static <A, B> Tuple2<A, B> of(A _1, B _2) {
    return new Tuple2<>(_1, _2);
  }

  static <A, B, C> Tuple3<A, B, C> of(A _1, B _2, C _3) {
    return new Tuple3<>(_1, _2, _3);
  }

  class Tuple2<A, B> {
    public final A _1;
    public final B _2;

    public Tuple2(A _1, B _2) {
      this._1 = _1;
      this._2 = _2;
    }
  }

  class Tuple3<A, B, C> extends Tuple2<A, B> {
    public final C _3;

    public Tuple3(A _1, B _2, C _3) {
      super(_1, _2);
      this._3 = _3;
    }
  }
}
