package dev.umut.wallet_api.account;

import java.util.List;

public record CursorPage<T>(List<T> content,String nextCursor) {
}
